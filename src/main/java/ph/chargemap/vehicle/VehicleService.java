package ph.chargemap.vehicle;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.user.User;
import ph.chargemap.user.UserRepository;
import ph.chargemap.user.UserService;

import java.time.Instant;
import java.util.List;

/**
 * CRUD for the authenticated user's embedded vehicle profiles (Requirement 10). All
 * operations are scoped to the current user, so a vehicle owned by another user is
 * simply not found (404) rather than accessible.
 */
@Service
public class VehicleService {

    private final UserService userService;
    private final UserRepository userRepository;

    public VehicleService(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    public List<VehicleDto> list() {
        return userService.requireCurrentUser().getVehicles().stream()
                .map(VehicleDto::from)
                .toList();
    }

    public VehicleDto create(VehicleRequest request) {
        User user = userService.requireCurrentUser();
        Instant now = Instant.now();
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(new ObjectId());
        apply(vehicle, request);
        vehicle.setCreatedAt(now);
        vehicle.setUpdatedAt(now);
        user.getVehicles().add(vehicle);
        touch(user);
        userRepository.save(user);
        return VehicleDto.from(vehicle);
    }

    public VehicleDto update(String vehicleId, VehicleRequest request) {
        User user = userService.requireCurrentUser();
        Vehicle vehicle = findOwned(user, vehicleId);
        apply(vehicle, request);
        vehicle.setUpdatedAt(Instant.now());
        touch(user);
        userRepository.save(user);
        return VehicleDto.from(vehicle);
    }

    public void delete(String vehicleId) {
        User user = userService.requireCurrentUser();
        ObjectId id = toObjectId(vehicleId);
        boolean removed = user.getVehicles().removeIf(v -> id.equals(v.getVehicleId()));
        if (!removed) {
            throw new NotFoundException("Vehicle not found: " + vehicleId);
        }
        touch(user);
        userRepository.save(user);
    }

    private Vehicle findOwned(User user, String vehicleId) {
        ObjectId id = toObjectId(vehicleId);
        return user.getVehicles().stream()
                .filter(v -> id.equals(v.getVehicleId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));
    }

    private void apply(Vehicle vehicle, VehicleRequest request) {
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setBatteryCapacityKwh(request.batteryCapacityKwh());
        vehicle.setConnectorType(request.connectorType());
        vehicle.setMaxAcKw(request.maxAcKw());
        vehicle.setMaxDcKw(request.maxDcKw());
    }

    private void touch(User user) {
        user.setUpdatedAt(Instant.now());
    }

    private ObjectId toObjectId(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid vehicle id: " + id);
        }
        return new ObjectId(id);
    }
}
