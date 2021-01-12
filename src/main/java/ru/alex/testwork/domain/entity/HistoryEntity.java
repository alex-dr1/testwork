package ru.alex.testwork.domain.entity;


import ru.alex.testwork.domain.xml.history.HistoryXml;

import javax.persistence.*;
import java.sql.Date;
import java.util.Objects;

@Entity
@Table(name = "history")
public class HistoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Date tradeDate;
	@Column(name = "sec_id", insertable = false, updatable = false)
	private String secId;
	private double numTrades;
	private double open;
	private double close;

	@ManyToOne
	@JoinColumn(name = "sec_id", referencedColumnName = "sec_id")
	private SecuritiesEntity securities;

	public HistoryEntity() {
	}

	public static HistoryEntity toEntity(HistoryXml historyXml){
		HistoryEntity history = new HistoryEntity();
		history.setSecId(historyXml.getSecId());
		history.setTradeDate(Date.valueOf(historyXml.getTradeDate()));
		history.setNumTrades(Double.parseDouble(historyXml.getNumTrades()));
		history.setOpen(Double.parseDouble(historyXml.getOpen()));
		history.setClose(Double.parseDouble(historyXml.getClose()));
		return history;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getTradeDate() {
		return tradeDate;
	}

	public void setTradeDate(Date tradeDate) {
		this.tradeDate = tradeDate;
	}

	public String getSecId() {
		return secId;
	}

	public void setSecId(String secId) {
		this.secId = secId;
	}

	public double getNumTrades() {
		return numTrades;
	}

	public void setNumTrades(double numTrades) {
		this.numTrades = numTrades;
	}

	public double getOpen() {
		return open;
	}

	public void setOpen(double open) {
		this.open = open;
	}

	public double getClose() {
		return close;
	}

	public void setClose(double close) {
		this.close = close;
	}

	public SecuritiesEntity getSecurities() {
		return securities;
	}

	public void setSecurities(SecuritiesEntity securities) {
		this.securities = securities;
	}

	@Override
	public String toString() {
		return "HistoryEntity{" +
				"id=" + id +
				", tradeDate=" + tradeDate +
				", secId='" + secId + '\'' +
				", numTrades=" + numTrades +
				", open=" + open +
				", close=" + close +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HistoryEntity)) return false;
		HistoryEntity history = (HistoryEntity) o;
		return Objects.equals(tradeDate, history.tradeDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tradeDate);
	}
}
