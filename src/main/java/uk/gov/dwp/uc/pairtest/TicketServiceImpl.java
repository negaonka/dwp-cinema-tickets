package uk.gov.dwp.uc.pairtest;

import java.math.BigDecimal;
import java.util.Arrays;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
	/**
	 * Should only have private methods other than the one below.
	 * 
	 */

	private final TicketPaymentService ticketPaymentService;
	private final SeatReservationService seatReservationService;

	TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
		this.ticketPaymentService = ticketPaymentService;
		this.seatReservationService = seatReservationService;
	}

	public TicketPaymentService getTicketPaymentService() {
		return ticketPaymentService;
	}

	public SeatReservationService getSeatReservationService() {
		return seatReservationService;
	}

	@Override
	public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
			throws InvalidPurchaseException {
		validateAccountAndTicketRequests(accountId, ticketTypeRequests);
		checkMaximumTickets(ticketTypeRequests);
		checkIfAdultTicketIsPurchased(ticketTypeRequests);
		processPayment(accountId, ticketTypeRequests);
		reserveSeat(accountId,ticketTypeRequests);
	}

	private void checkMaximumTickets(TicketTypeRequest[] ticketTypeRequests) {
		int sumOfTickets = Arrays.asList(ticketTypeRequests).stream().reduce(0,
				(tickets, ticketTypRequest) -> tickets + ticketTypRequest.getNoOfTickets(), Integer::sum);
		if (sumOfTickets > 20) {
			throw new InvalidPurchaseException("Maximum of 20 tickets can be purchased at a time");
		}
	}

	private void validateAccountAndTicketRequests(Long accountId, TicketTypeRequest[] ticketTypeRequests) {
		if (accountId == null || accountId <= 0) {
			throw new InvalidPurchaseException("Account Id id not valid");
		}
		if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
			throw new InvalidPurchaseException("Ticket type is needed to purchase the tickets");
		}
	}
	
	private void checkIfAdultTicketIsPurchased(TicketTypeRequest[] ticketTypeRequests) {
		if(!Arrays.stream(ticketTypeRequests).anyMatch(ticketTypeRequest -> ticketTypeRequest.getTicketType().equals(Type.ADULT) && ticketTypeRequest.getNoOfTickets() > 0)) {
			throw new InvalidPurchaseException("Atleast one adult ticket should be present in the request");
		}
	}
	
	private void processPayment(Long accountId, TicketTypeRequest[] ticketTypeRequests) {
		int totalPrice = Arrays.stream(ticketTypeRequests).map(ticket -> ticket.getNoOfTickets() * ticket.getTicketType().getPrice())
				.mapToInt(Integer::intValue).sum();
		ticketPaymentService.makePayment(accountId, totalPrice);
		
	}
	
	private void reserveSeat(Long accountId, TicketTypeRequest[] ticketTypeRequests) {
		int totalSeatsToReserve = Arrays.stream(ticketTypeRequests).map(ticket -> ticket .getTicketType() != Type.INFANT ? ticket.getNoOfTickets(): 0)
				.mapToInt(Integer::intValue).sum();
		seatReservationService.reserveSeat(accountId, totalSeatsToReserve);
	}


}
