package com.cdvtc.demo.service;

import com.cdvtc.demo.dto.TrainPreviewResponse;
import com.cdvtc.demo.dto.TrainSaveRequest;
import com.cdvtc.demo.entity.Carriage;
import com.cdvtc.demo.entity.Seat;
import com.cdvtc.demo.entity.Station;
import com.cdvtc.demo.entity.TrainSchedule;
import com.cdvtc.demo.entity.TrainStation;
import com.cdvtc.demo.entity.TrainStationPrice;
import com.cdvtc.demo.entity.TrainStationRelation;
import com.cdvtc.demo.mapper.CarriageMapper;
import com.cdvtc.demo.mapper.OrderMapper;
import com.cdvtc.demo.mapper.ScheduleSeatMapper;
import com.cdvtc.demo.mapper.SeatMapper;
import com.cdvtc.demo.mapper.SeatSegmentOccupancyMapper;
import com.cdvtc.demo.mapper.StationMapper;
import com.cdvtc.demo.mapper.TicketMapper;
import com.cdvtc.demo.mapper.TrainMapper;
import com.cdvtc.demo.mapper.TrainScheduleMapper;
import com.cdvtc.demo.mapper.TrainStationMapper;
import com.cdvtc.demo.mapper.TrainStationPriceMapper;
import com.cdvtc.demo.mapper.TrainStationRelationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainAdminPreviewTest {

    @Mock private TrainMapper trainMapper;
    @Mock private StationMapper stationMapper;
    @Mock private TrainStationMapper trainStationMapper;
    @Mock private TrainStationPriceMapper trainStationPriceMapper;
    @Mock private TrainStationRelationMapper trainStationRelationMapper;
    @Mock private CarriageMapper carriageMapper;
    @Mock private SeatMapper seatMapper;
    @Mock private ScheduleSeatMapper scheduleSeatMapper;
    @Mock private SeatSegmentOccupancyMapper occupancyMapper;
    @Mock private TrainScheduleMapper trainScheduleMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private TicketMapper ticketMapper;

    @InjectMocks private TrainAdminService service;

    @Test
    void previewCalculatesTemplateWithoutWritingAnyChildRecord() {
        when(trainMapper.selectCount(any())).thenReturn(0L);
        TrainSaveRequest request = new TrainSaveRequest();
        request.setTrainNumber("G-PREVIEW");
        request.setDepartureTime(LocalDateTime.of(2026, 10, 22, 8, 0));
        request.setArrivalTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        request.setRunStartDate(LocalDate.of(2026, 10, 22));
        request.setRunEndDate(LocalDate.of(2026, 10, 22));
        request.setStops(List.of(stop("01", 1L, "甲", 0), stop("02", 2L, "乙", 10), stop("03", 3L, "丙", 0)));
        when(stationMapper.selectById(1L)).thenReturn(station(1L, "甲"));
        when(stationMapper.selectById(2L)).thenReturn(station(2L, "乙"));
        when(stationMapper.selectById(3L)).thenReturn(station(3L, "丙"));
        TrainSaveRequest.Fare fare = new TrainSaveRequest.Fare();
        fare.setSeatType(2);
        fare.setPrice(6000);
        request.setFares(List.of(fare));
        TrainSaveRequest.CarriageItem carriage = new TrainSaveRequest.CarriageItem();
        carriage.setCarriageNumber("01");
        carriage.setCarriageType(2);
        carriage.setSeatCount(20);
        request.setCarriages(List.of(carriage));

        TrainPreviewResponse response = service.preview(request);

        assertEquals("甲", response.getStartStation());
        assertEquals("丙", response.getEndStation());
        assertEquals(120, response.getTotalMinutes());
        assertEquals(3, response.getStops().size());
        verify(trainStationMapper, never()).insert(any(TrainStation.class));
        verify(trainStationPriceMapper, never()).insert(any(TrainStationPrice.class));
        verify(trainStationRelationMapper, never()).insert(any(TrainStationRelation.class));
        verify(trainScheduleMapper, never()).insert(any(TrainSchedule.class));
        verify(carriageMapper, never()).insert(any(Carriage.class));
        verify(seatMapper, never()).insert(any(Seat.class));
    }

    @Test
    void previewRejectsUnknownStation() {
        when(trainMapper.selectCount(any())).thenReturn(0L);
        when(stationMapper.selectById(99L)).thenReturn(null);
        TrainSaveRequest request = new TrainSaveRequest();
        request.setTrainNumber("G-UNKNOWN-STATION");
        request.setDepartureTime(LocalDateTime.of(2026, 10, 22, 8, 0));
        request.setArrivalTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        request.setRunStartDate(LocalDate.of(2026, 10, 22));
        request.setRunEndDate(LocalDate.of(2026, 10, 22));
        request.setStops(List.of(stop("01", 99L, "不存在站", 0), stop("02", 99L, "不存在站", 0)));
        TrainSaveRequest.CarriageItem carriage = new TrainSaveRequest.CarriageItem();
        carriage.setCarriageNumber("01");
        carriage.setCarriageType(2);
        carriage.setSeatCount(20);
        request.setCarriages(List.of(carriage));

        assertThrows(IllegalArgumentException.class, () -> service.preview(request));
    }

    private static Station station(Long id, String name) {
        Station station = new Station();
        station.setId(id);
        station.setName(name);
        return station;
    }

    private static TrainSaveRequest.Stop stop(String sequence, Long stationId, String station, int stopover) {
        TrainSaveRequest.Stop stop = new TrainSaveRequest.Stop();
        stop.setSequence(sequence);
        stop.setStationId(stationId);
        stop.setDeparture(station);
        stop.setStopoverTime(stopover);
        return stop;
    }
}
