package com.pixsom.racebets.admin.race;

import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.race.dto.RaceRequest;
import com.pixsom.racebets.admin.race.dto.RaceResponse;
import com.pixsom.racebets.entities.Race;
import com.pixsom.racebets.enums.RaceState;
import com.pixsom.racebets.repositories.RaceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RaceService {

    private final RaceRepository raceRepository;

    public RaceService(RaceRepository raceRepository) {
        this.raceRepository = raceRepository;
    }

    @Transactional(readOnly = true)
    public List<RaceResponse> findAll() {
        return raceRepository.findAll(Sort.by("id").ascending())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RaceResponse findById(Long id) {
        return toResponse(findRace(id));
    }

    @Transactional
    public RaceResponse create(RaceRequest request) {
        Race race = new Race();
        applyRequest(race, request, RaceState.CREATED);
        return toResponse(raceRepository.save(race));
    }

    @Transactional
    public RaceResponse update(Long id, RaceRequest request) {
        Race race = findRace(id);
        applyRequest(race, request, race.getState());
        return toResponse(raceRepository.save(race));
    }

    @Transactional
    public void delete(Long id) {
        Race race = findRace(id);
        raceRepository.delete(race);
    }

    private Race findRace(Long id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Race not found"));
    }

    private void applyRequest(Race race, RaceRequest request, RaceState defaultState) {
        race.setName(request.name().trim());
        race.setRaceImgUrl(normalizeOptionalText(request.raceImgUrl()));
        race.setState(defaultState);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private RaceResponse toResponse(Race race) {
        return new RaceResponse(race.getId(), race.getName(), race.getRaceImgUrl(), race.getState());
    }
}
