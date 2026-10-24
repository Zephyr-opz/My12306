package com.cdvtc.demo.service;

import com.cdvtc.demo.entity.TrainSchedule;
import com.cdvtc.demo.mapper.TicketMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketDuplicatePurchaseTest {

    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketPurchaseService service;

    @Test
    void rejectsPassengerWithExistingActiveTicketForSameSchedule() {
        TrainSchedule schedule = new TrainSchedule();
        schedule.setId(88L);
        when(ticketMapper.selectCount(any())).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
                () -> service.ensurePassengerNotBooked(schedule, 42L));
    }
}
