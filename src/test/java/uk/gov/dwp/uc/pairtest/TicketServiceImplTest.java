package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

@RunWith(MockitoJUnitRunner.class)
public class TicketServiceImplTest {

	@Mock
	private TicketPaymentService ticketPaymentService;
	@Mock
	private SeatReservationService seatReservationService;

	private TicketServiceImpl ticketServiceImpl;

	@Before
	public void setUp() {
		ticketServiceImpl = new TicketServiceImpl(ticketPaymentService, seatReservationService);
	}

	@Test
	public void testPurchaseMaximumTickets() {
		InvalidPurchaseException thrown = assertThrows(InvalidPurchaseException.class,
				() -> ticketServiceImpl.purchaseTickets(40L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 30)));
		assertTrue(thrown.getMessage().equalsIgnoreCase("Maximum of 20 tickets can be purchased at a time"));
	}

	@Test
	public void testAccountIsNotValid() {
		InvalidPurchaseException thrown = assertThrows(InvalidPurchaseException.class,
				() -> ticketServiceImpl.purchaseTickets(0L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 30)));
		assertTrue(thrown.getMessage().equalsIgnoreCase("Account Id id not valid"));
	}

	@Test
	public void testTicketRequest() {
		InvalidPurchaseException thrown = assertThrows(InvalidPurchaseException.class,
				() -> ticketServiceImpl.purchaseTickets(40L, new TicketTypeRequest[0]));
		assertTrue(thrown.getMessage().equalsIgnoreCase("Ticket type is needed to purchase the tickets"));
	}

	@Test
	public void testAdultIsPresent() {
		InvalidPurchaseException thrown = assertThrows(InvalidPurchaseException.class,
				() -> ticketServiceImpl.purchaseTickets(40L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0),
						new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10)));
		assertTrue(thrown.getMessage().equalsIgnoreCase("Atleast one adult ticket should be present in the request"));
	}

	@Test
	public void testHappyFlow() {
		Mockito.doNothing().when(ticketPaymentService).makePayment(ArgumentMatchers.anyLong(),
				ArgumentMatchers.anyInt());
		Mockito.doNothing().when(seatReservationService).reserveSeat(ArgumentMatchers.anyLong(),
				ArgumentMatchers.anyInt());

		ticketServiceImpl.purchaseTickets(50L, new TicketTypeRequest(Type.ADULT, 10),
                new TicketTypeRequest(Type.CHILD, 5), new TicketTypeRequest(Type.INFANT, 5));

		Mockito.verify(ticketPaymentService).makePayment(50, 250);
		Mockito.verify(seatReservationService).reserveSeat(50, 15);
	}


}