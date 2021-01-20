package ru.alex.testwork.camelrouters;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.alex.testwork.mapper.SecuritiesMapper;
import ru.alex.testwork.xml.securities.SecuritiesXml;

@Component
public class MoexFetchSecuritiesRouter extends RouteBuilder {

	@Autowired
	private final JaxbDataFormat jaxbSec;

	public MoexFetchSecuritiesRouter(JaxbDataFormat jaxbSec) {
		this.jaxbSec = jaxbSec;
	}

	private final Processor convertXmlToSecuritiesEntity = exchange -> {
		SecuritiesXml xml = exchange.getIn().getBody(SecuritiesXml.class);
		if(xml == null){
			exchange.getIn().setBody(null);
		} else {
			exchange.getIn().setBody(SecuritiesMapper.xmlToEntity(xml));
		}
	};

	@Override
	public void configure() throws Exception {
		// Http request to iss.moex.com
		from("direct:fetchSecuritiesMoexService").routeId("Route fetchSecuritiesMoexService")
				.setProperty("secId", simple("${body}"))
				.setHeader("secId", exchangeProperty("secId"))
				.setHeader(Exchange.HTTP_QUERY, simple("iss.meta=off&securities.columns=secid,name,regnumber,emitent_title&q=${headers.secId}"))
				//https://iss.moex.com/iss/securities.xml?iss.meta=off&securities.columns=secid,name,regnumber,emitent_title&q=AFKS
				.doTry()
				.to("https://iss.moex.com/iss/securities.xml?httpMethod=GET")
				.doCatch(CamelException.class)
				.log(LoggingLevel.ERROR, "${exchangeProperty.CamelExceptionCaught}")
				.to("direct:faultMoexService")
				.end()
				.to("direct:validXmlMoexService");

		//Valid XML
		from("direct:validXmlMoexService").routeId("Route validXmlMoexService")
				.convertBodyTo(String.class)
				.doTry()
				.to("validator:xsd/securities.xsd")
				.doCatch(ValidationException.class)
				.log(LoggingLevel.ERROR, "MoexService: Failed validation xml")
				.to("direct:faultMoexService")
				.end().to("direct:unmarshalMoexService");

		//Unmarshal
		from("direct:unmarshalMoexService").routeId("Route unmarshalMoexService")
				.setHeader("secId", exchangeProperty("secId"))
				.transform(xpath("//document/data[@id='securities']/rows/row[@secid=function:simple('${headers.secId}')]"))
				// TODO DELETE ---------------------------------
				//.convertBodyTo(String.class)
				//.wireTap("direct:saveToFile")
				// TODO DELETE ---------------------------------
				.unmarshal(jaxbSec)
				.process(convertXmlToSecuritiesEntity)
		;

		// TODO DELETE ---------------------------------
		/*from("direct:saveToFile").routeId("saveToFile")
				.process(exchange -> {
					String body = exchange.getIn().getBody(String.class) + "\n";
					exchange.getIn().setBody(body);
				})
				.to("file:outdir?fileName=securities.xml&fileExist=Append")
				.stop();*/
		// TODO DELETE ---------------------------------

		// route in case of error
		from("direct:faultMoexService")
				.routeId("Route faultMoexService")
				.transform().body().setBody(constant(null))
				.log("return: body = null")
				.stop();
	}
}
