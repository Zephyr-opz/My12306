package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.dto.RegionSaveRequest;
import com.cdvtc.demo.dto.StationSaveRequest;
import com.cdvtc.demo.entity.Region;
import com.cdvtc.demo.entity.Station;
import com.cdvtc.demo.entity.TrainStation;
import com.cdvtc.demo.mapper.RegionMapper;
import com.cdvtc.demo.mapper.StationMapper;
import com.cdvtc.demo.mapper.TrainStationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionStationAdminService {

    private final RegionMapper regionMapper;
    private final StationMapper stationMapper;
    private final TrainStationMapper trainStationMapper;

    public List<Region> listRegions(String keyword) {
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Region::getName, keyword)
                    .or().like(Region::getFullName, keyword)
                    .or().like(Region::getCode, keyword);
        }
        return regionMapper.selectList(wrapper.orderByAsc(Region::getName));
    }

    public Region createRegion(RegionSaveRequest request) {
        ensureRegionUnique(request, null);
        Region region = new Region();
        BeanUtils.copyProperties(request, region);
        if (region.getPopularFlag() == null) region.setPopularFlag(0);
        regionMapper.insert(region);
        return region;
    }

    public Region updateRegion(Long id, RegionSaveRequest request) {
        Region region = regionMapper.selectById(id);
        if (region == null) throw new IllegalArgumentException("地区不存在");
        ensureRegionUnique(request, id);
        BeanUtils.copyProperties(request, region);
        region.setId(id);
        regionMapper.updateById(region);
        return region;
    }

    public void deleteRegion(Long id) {
        Region region = regionMapper.selectById(id);
        if (region == null) throw new IllegalArgumentException("地区不存在");
        if (stationMapper.selectCount(new LambdaQueryWrapper<Station>()
                .eq(Station::getRegion, region.getCode())
                .or().eq(Station::getRegionName, region.getName())) > 0) {
            throw new IllegalArgumentException("该地区仍有车站，不能删除");
        }
        regionMapper.deleteById(id);
    }

    public List<Station> listStations(String keyword) {
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Station::getName, keyword)
                    .or().like(Station::getCode, keyword)
                    .or().like(Station::getSpell, keyword)
                    .or().like(Station::getRegionName, keyword);
        }
        return stationMapper.selectList(wrapper.orderByAsc(Station::getName));
    }

    public Station createStation(StationSaveRequest request) {
        ensureStationUnique(request, null);
        ensureRegionExists(request);
        Station station = new Station();
        BeanUtils.copyProperties(request, station);
        stationMapper.insert(station);
        return station;
    }

    public Station updateStation(Long id, StationSaveRequest request) {
        Station station = stationMapper.selectById(id);
        if (station == null) throw new IllegalArgumentException("车站不存在");
        ensureStationUnique(request, id);
        ensureRegionExists(request);
        BeanUtils.copyProperties(request, station);
        station.setId(id);
        stationMapper.updateById(station);
        return station;
    }

    public void deleteStation(Long id) {
        Station station = stationMapper.selectById(id);
        if (station == null) throw new IllegalArgumentException("车站不存在");
        if (trainStationMapper.selectCount(new LambdaQueryWrapper<TrainStation>()
                .eq(TrainStation::getDeparture, station.getName())
                .or().eq(TrainStation::getArrival, station.getName())) > 0) {
            throw new IllegalArgumentException("该车站已被车次使用，不能删除");
        }
        stationMapper.deleteById(id);
    }

    private void ensureRegionUnique(RegionSaveRequest request, Long currentId) {
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<Region>()
                .and(w -> w.eq(Region::getName, request.getName()).or().eq(Region::getCode, request.getCode()));
        if (currentId != null) wrapper.ne(Region::getId, currentId);
        if (regionMapper.selectCount(wrapper) > 0) throw new IllegalArgumentException("地区名称或编码已存在");
    }

    private void ensureStationUnique(StationSaveRequest request, Long currentId) {
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<Station>()
                .and(w -> w.eq(Station::getName, request.getName()).or().eq(Station::getCode, request.getCode()));
        if (currentId != null) wrapper.ne(Station::getId, currentId);
        if (stationMapper.selectCount(wrapper) > 0) throw new IllegalArgumentException("车站名称或编码已存在");
    }

    private void ensureRegionExists(StationSaveRequest request) {
        if (!StringUtils.hasText(request.getRegion())) return;
        Region region = regionMapper.selectOne(new LambdaQueryWrapper<Region>().eq(Region::getCode, request.getRegion()));
        if (region == null) throw new IllegalArgumentException("所属地区不存在，请先维护地区");
        request.setRegionName(region.getName());
    }
}
