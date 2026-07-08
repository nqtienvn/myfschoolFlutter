package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.CreateRoomRequest;
import vn.edu.fpt.myfschool.common.dto.RoomDto;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.Room;
import vn.edu.fpt.myfschool.repository.RoomRepository;
import vn.edu.fpt.myfschool.service.RoomService;

import java.util.List;

@Service("roomService")
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomDto> listRooms() {
        return roomRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public RoomDto createRoom(CreateRoomRequest request) {
        if (roomRepository.existsByName(request.name().trim())) {
            throw new ConflictException("Tên phòng học đã tồn tại");
        }

        Room room = new Room();
        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
        room.setBuilding(trim(request.building()));
        room.setEquipment(trim(request.equipment()));
        room.setIsActive(request.isActive() != null ? request.isActive() : true);
        return toDto(roomRepository.save(room));
    }

    @Override
    public RoomDto updateRoom(Long id, CreateRoomRequest request) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room", "id", id));

        roomRepository.findByName(request.name().trim())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new ConflictException("Tên phòng học đã tồn tại ở phòng khác"); });

        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
        room.setBuilding(trim(request.building()));
        room.setEquipment(trim(request.equipment()));
        room.setIsActive(request.isActive() != null ? request.isActive() : true);
        return toDto(roomRepository.save(room));
    }

    @Override
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room", "id", id));
        roomRepository.delete(room);
    }

    private RoomDto toDto(Room room) {
        return new RoomDto(room.getId(), room.getName(), room.getCapacity(), room.getBuilding(), room.getEquipment(), room.getIsActive());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
