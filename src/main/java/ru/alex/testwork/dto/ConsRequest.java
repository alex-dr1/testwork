package ru.alex.testwork.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ConsRequest {

	String filterEmitentTitle;
	String filterTradeDate;
	Map<String, String> sortField;

}