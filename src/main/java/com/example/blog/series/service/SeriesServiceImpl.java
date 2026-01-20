package com.example.blog.series.service;

import com.example.blog.series.presentation.dto.response.SeriesGetDto;
import com.example.blog.series.repository.SeriesRepository;
import com.example.blog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeriesServiceImpl implements SeriesService {
    private final SeriesRepository seriesRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SeriesGetDto> getMySeries(Long userId) {
        return seriesRepository.findAllByOwner_UserIdOrderByIdDesc(userId).stream()
                .map(SeriesGetDto::from)
                .toList();
    }
}
