package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.CreateRoomRequest;
import vn.edu.fpt.myfschool.common.dto.RoomDto;

import java.util.List;

public interface RoomService {
    List<RoomDto> listRooms();
    RoomDto createRoom(CreateRoomRequest request);
    RoomDto updateRoom(Long id, CreateRoomRequest request);
    void deleteRoom(Long id);
}
