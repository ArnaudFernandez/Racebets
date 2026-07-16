package com.pixsom.racebets.admin.raceentry;

import com.pixsom.racebets.admin.ConflictException;
import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.raceentry.dto.RaceEntryRequest;
import com.pixsom.racebets.admin.raceentry.dto.RaceEntryResponse;
import com.pixsom.racebets.entities.Horse;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.entities.RaceEntry;
import com.pixsom.racebets.repositories.HorseRepository;
import com.pixsom.racebets.repositories.RaceEntryRepository;
import com.pixsom.racebets.repositories.RaceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RaceEntryService {

    private final RaceEntryRepository raceEntryRepository;
    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;

    public RaceEntryService(RaceEntryRepository raceEntryRepository, RaceRepository raceRepository, HorseRepository horseRepository) {
        this.raceEntryRepository = raceEntryRepository;
        this.raceRepository = raceRepository;
        this.horseRepository = horseRepository;
    }

    @Transactional(readOnly = true)
    public List<RaceEntryResponse> findAll(Long raceId) {
        List<RaceEntry> entries = raceId == null
                ? raceEntryRepository.findAll(defaultSort())
                : raceEntryRepository.findAllByRace_Id(raceId, Sort.by("horseNumber").ascending());

        return entries.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RaceEntryResponse findById(Long id) {
        return toResponse(findRaceEntry(id));
    }

    @Transactional
    public RaceEntryResponse create(RaceEntryRequest request) {
        Race race = findRace(request.raceId());
        ensureDraft(race);
        Horse horse = findHorse(request.horseId());
        validateCreateUniqueness(request);

        RaceEntry raceEntry = new RaceEntry();
        applyRequest(raceEntry, race, horse, request);
        return toResponse(raceEntryRepository.save(raceEntry));
    }

    @Transactional
    public RaceEntryResponse update(Long id, RaceEntryRequest request) {
        RaceEntry raceEntry = findRaceEntry(id);
        Race race = findRace(request.raceId());
        ensureDraft(race);
        Horse horse = findHorse(request.horseId());
        validateUpdateUniqueness(id, request);

        applyRequest(raceEntry, race, horse, request);
        return toResponse(raceEntryRepository.save(raceEntry));
    }

    @Transactional
    public void delete(Long id) {
        RaceEntry raceEntry = findRaceEntry(id);
        ensureDraft(raceEntry.getRace());
        raceEntryRepository.delete(raceEntry);
    }

    private void validateCreateUniqueness(RaceEntryRequest request) {
        if (raceEntryRepository.existsByRace_IdAndHorse_Id(request.raceId(), request.horseId())) {
            throw new ConflictException("Horse is already registered for this race");
        }
        if (raceEntryRepository.existsByRace_IdAndHorseNumber(request.raceId(), request.horseNumber())) {
            throw new ConflictException("Horse number is already used for this race");
        }
    }

    private void validateUpdateUniqueness(Long id, RaceEntryRequest request) {
        if (raceEntryRepository.existsByRace_IdAndHorse_IdAndIdNot(request.raceId(), request.horseId(), id)) {
            throw new ConflictException("Horse is already registered for this race");
        }
        if (raceEntryRepository.existsByRace_IdAndHorseNumberAndIdNot(request.raceId(), request.horseNumber(), id)) {
            throw new ConflictException("Horse number is already used for this race");
        }
    }

    private RaceEntry findRaceEntry(Long id) {
        return raceEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Race entry not found"));
    }

    private Race findRace(Long id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Race not found"));
    }

    private Horse findHorse(Long id) {
        return horseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Horse not found"));
    }

    private void applyRequest(RaceEntry raceEntry, Race race, Horse horse, RaceEntryRequest request) {
        raceEntry.setRace(race);
        raceEntry.setHorse(horse);
        raceEntry.setHorseNumber(request.horseNumber());
    }

    private void ensureDraft(Race race) {
        if (race.getState() != com.pixsom.racebets.enums.RaceState.CREATED) {
            throw new ConflictException("Runners can only be changed while the race is a draft");
        }
    }

    private RaceEntryResponse toResponse(RaceEntry raceEntry) {
        return new RaceEntryResponse(
                raceEntry.getId(),
                raceEntry.getRace().getId(),
                raceEntry.getRace().getName(),
                raceEntry.getHorse().getId(),
                raceEntry.getHorse().getName(),
                raceEntry.getHorseNumber(),
                raceEntry.getRank()
        );
    }

    private Sort defaultSort() {
        return Sort.by(Sort.Order.asc("race.name"), Sort.Order.asc("horseNumber"));
    }
}
