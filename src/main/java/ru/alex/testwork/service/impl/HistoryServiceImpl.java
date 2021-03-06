package ru.alex.testwork.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.alex.testwork.controller.dto.HistoryDto;
import ru.alex.testwork.entity.History;
import ru.alex.testwork.entity.Securities;
import ru.alex.testwork.exception.BadRestRequestException;
import ru.alex.testwork.exception.HistoryNotFoundException;
import ru.alex.testwork.exception.SecuritiesBySecIdNotFoundException;
import ru.alex.testwork.mapper.HistoryMapper;
import ru.alex.testwork.repository.HistoryRepo;
import ru.alex.testwork.repository.SecuritiesRepo;
import ru.alex.testwork.service.HistoryService;
import ru.alex.testwork.service.MoexService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HistoryServiceImpl implements HistoryService {
	final HistoryRepo historyRepo;
	final SecuritiesRepo securitiesRepo;
	final MoexService moexService;

	public HistoryServiceImpl(HistoryRepo historyRepo, SecuritiesRepo securitiesRepo, MoexService moexService) {
		this.historyRepo = historyRepo;
		this.securitiesRepo = securitiesRepo;
		this.moexService = moexService;
	}

	public void saveImport(History history) {
		try {
			String secId = history.getSecId();
			Optional<Securities> securities = securitiesRepo.findSecuritiesBySecId(secId);
			Securities securitiesSave = securities.orElseGet(() -> goToMoex(secId));
			history.setSecurities(securitiesSave);
			historyRepo.save(history);
		}catch (SecuritiesBySecIdNotFoundException exception){
			log.error("saveImport(): " + exception.getMessage());
		}
	}

	private Securities goToMoex(String secId) {
		Optional<Securities> securitiesMoex = moexService.fetchSecuritiesBySecId(secId);
		Securities securitiesSave = securitiesMoex.orElseThrow(() -> new SecuritiesBySecIdNotFoundException(secId));
		return securitiesRepo.save(securitiesSave);
	}

	@Override
	public HistoryDto findHistoryById(Long id) {
		History entity = historyRepo.findById(id).orElseThrow(() -> new HistoryNotFoundException(id));
		return HistoryMapper.entityToDto(entity);
	}

	@Override
	public List<HistoryDto> findAllHistoryBySecId(String secId) {
		return historyRepo.findAllBySecId(secId).stream()
				.map(HistoryMapper::entityToDto)
				.collect(Collectors.toList());
	}

	@Override
	public HistoryDto create(HistoryDto dto) {
		Optional<Long> id = Optional.ofNullable(dto.getId());
		Optional<String> secId = Optional.ofNullable(dto.getSecId());
		History entity = HistoryMapper.dtoToEntity(dto);

		id.ifPresent(aLong -> {
			throw new BadRestRequestException("Create error: id != null");
		});
		secId.orElseThrow(() -> new BadRestRequestException("Create error: secId == null"));

		Securities securities = securitiesRepo.findSecuritiesBySecId(secId.get()).orElseThrow(() -> new SecuritiesBySecIdNotFoundException(secId.get()));

		entity.setSecurities(securities);
		return HistoryMapper.entityToDto(historyRepo.save(entity));
	}

	@Override
	public HistoryDto update(HistoryDto dto) {
		Optional<Long> id = Optional.ofNullable(dto.getId());
		Optional<String> secId = Optional.ofNullable(dto.getSecId());
		History entity = HistoryMapper.dtoToEntity(dto);

		id.orElseThrow(() -> new BadRestRequestException("Update error: id == null"));
		secId.orElseThrow(() -> new BadRestRequestException("Update error: secId == null"));

		Securities securities = securitiesRepo.findSecuritiesBySecId(secId.get()).orElseThrow(() -> new SecuritiesBySecIdNotFoundException(secId.get()));
		entity.setSecurities(securities);

		return HistoryMapper.entityToDto(historyRepo.save(entity));
	}

	@Override
	public Long delete(Long id) {
		if (!historyRepo.existsById(id)) throw new HistoryNotFoundException(id);
		historyRepo.deleteById(id);
		return id;
	}


}
