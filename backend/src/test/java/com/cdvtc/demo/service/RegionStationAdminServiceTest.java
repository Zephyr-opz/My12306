package com.cdvtc.demo.service;

import com.cdvtc.demo.dto.RegionSaveRequest;
import com.cdvtc.demo.dto.StationSaveRequest;
import com.cdvtc.demo.entity.Region;
import com.cdvtc.demo.entity.Station;
import com.cdvtc.demo.mapper.RegionMapper;
import com.cdvtc.demo.mapper.StationMapper;
import com.cdvtc.demo.mapper.TrainStationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionStationAdminServiceTest {

    @Mock private RegionMapper regionMapper;
    @Mock private StationMapper stationMapper;
    @Mock private TrainStationMapper trainStationMapper;
    @InjectMocks private RegionStationAdminService service;

    @Test
    void rejectsDuplicateRegionCode() {
        when(regionMapper.selectCount(any())).thenReturn(1L);
        RegionSaveRequest request = new RegionSaveRequest();
        request.setName("华东");
        request.setCode("east");

        assertThrows(IllegalArgumentException.class, () -> service.createRegion(request));
    }

    @Test
    void rejectsDeletingReferencedStation() {
        Station station = new Station();
        station.setId(7L);
        station.setName("大连北站");
        when(stationMapper.selectById(7L)).thenReturn(station);
        when(trainStationMapper.selectCount(any())).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> service.deleteStation(7L));
    }

    @Test
    void createsStationWithRegionInformation() {
        when(stationMapper.selectCount(any())).thenReturn(0L);
        Region region = new Region();
        region.setCode("liaoning");
        region.setName("辽宁");
        when(regionMapper.selectOne(any())).thenReturn(region);
        StationSaveRequest request = new StationSaveRequest();
        request.setCode("DDB");
        request.setName("大连北站");
        request.setRegion("liaoning");
        request.setRegionName("辽宁");

        Station result = service.createStation(request);

        assertEquals("DDB", result.getCode());
        assertEquals("大连北站", result.getName());
        assertEquals("辽宁", result.getRegionName());
    }
}
