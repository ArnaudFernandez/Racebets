package com.pixsom.racebets.admin.horse;

import com.pixsom.racebets.admin.NotFoundException;
import com.pixsom.racebets.admin.horse.dto.HorseRequest;
import com.pixsom.racebets.admin.horse.dto.HorseResponse;
import com.pixsom.racebets.entities.Horse;
import com.pixsom.racebets.repositories.HorseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HorseService {

    private final HorseRepository horseRepository;

    public HorseService(HorseRepository horseRepository) {
        this.horseRepository = horseRepository;
    }

    @Transactional(readOnly = true)
    public List<HorseResponse> findAll() {
        return horseRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HorseResponse findById(Long id) {
        return toResponse(findHorse(id));
    }

    @Transactional
    public HorseResponse create(HorseRequest request) {
        Horse horse = new Horse();
        horse.setName(request.name().trim());
        return toResponse(horseRepository.save(horse));
    }

    @Transactional
    public HorseResponse update(Long id, HorseRequest request) {
        Horse horse = findHorse(id);
        horse.setName(request.name().trim());
        return toResponse(horseRepository.save(horse));
    }

    @Transactional
    public void delete(Long id) {
        Horse horse = findHorse(id);
        horseRepository.delete(horse);
    }

    private Horse findHorse(Long id) {
        return horseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Horse not found"));
    }

    private HorseResponse toResponse(Horse horse) {
        return new HorseResponse(horse.getId(), horse.getName());
    }
}
