package ru.alex.testwork.camelrouters;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.stereotype.Component;
import ru.alex.testwork.entity.History;
import ru.alex.testwork.mapper.HistoryMapper;
import ru.alex.testwork.xml.history.HistoryListXml;
import ru.alex.testwork.xml.history.HistoryXml;
import ru.alex.testwork.service.impl.HistoryServiceImpl;
import ru.alex.testwork.utils.FileFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ParseHistoryRouter extends RouteBuilder {

	final JaxbDataFormat jaxbListHis;
	final HistoryServiceImpl historyService;

	public ParseHistoryRouter(JaxbDataFormat jaxbListHis, HistoryServiceImpl historyService) {
		this.jaxbListHis = jaxbListHis;
		this.historyService = historyService;
	}

	Processor fileHistoryAmount = exchange -> {
		FileFinder finder = new FileFinder("inbox/history", "history_[0-9]*.xml");
		int amount = finder.done();
		exchange.getIn().setBody(amount, Integer.class);
	};

	Processor covertListXmlToHistory = exchange -> {
		List<HistoryXml> historyXmlList = exchange.getIn().getBody(HistoryListXml.class).getHistoryXmlList();
		List<History> historyList = historyXmlList.stream()
				.filter(historyXml -> !historyXml.getNumTrades().equals("0"))
				.map(convertHistoryXmlToHistory())
				.collect(Collectors.toList());
		exchange.getIn().setBody(historyList);
	};

	private Function<HistoryXml, History> convertHistoryXmlToHistory() {
		return HistoryMapper::xmlToEntity;
	}

	@Override
	public void configure() throws Exception {
		from("direct:parseHistory").routeId("Route parseHistory")
				.log("Run parseHistory ...")
				.process(fileHistoryAmount)
				.choice()
				.when(simple("${body} > 0")).to("direct:fileLoopHistory")
				.otherwise().log("file history amount = 0").to("direct:faultHistory")
				.end()
				.log("Stop parseHistory ...")
		;
		//File loop
		//TODO если файл не валид следующий не берет
		from("direct:fileLoopHistory").routeId("Router fileLoopHistory")
				.loop(simple("${body}"))
				.pollEnrich("file://inbox/history?include=history_[0-9]*.xml&noop=true")
				.to("direct:validXmlHistory")
				.end()
		;

		//Valid XML
		from("direct:validXmlHistory").routeId("Route validXmlHistory")
				.convertBodyTo(String.class)
				.doTry()
				.to("validator:xsd/history.xsd")
				.doCatch(ValidationException.class)
				.log(LoggingLevel.ERROR, "Failed validation xml History")
				.to("direct:faultHistory")
				.end().to("direct:unmarshalHistory")
		;
		// Unmarshal history
		from("direct:unmarshalHistory")
				.transform(xpath("//document/data[@id='history']/rows"))
				.unmarshal(jaxbListHis)
				.process(covertListXmlToHistory)
				.process(exchange -> {
					List<History> historyList = exchange.getIn().getBody(ArrayList.class);
					//TODO Insert to DB
					historyList.forEach(historyService::saveImport);
				})
				.setBody().simple("stop parseHistory")
		;

		// route in case of error
		from("direct:faultHistory")
				.routeId("Route faultHistory")
				.transform().body().setBody(constant("stop fault"))
				.log("return: body = stop fault")
				.stop();
	}

}
