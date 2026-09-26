package ph.chargemap.branch;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ph.chargemap.branch.BranchService.BranchRequest;

import java.util.List;
import java.util.Map;

/**
 * SUPER_ADMIN-only management: branches (CRUD + images + assignment) and admin oversight
 * (list, reassign branch, disable/enable). Guarded by {@code /api/superadmin/**}.
 */
@RestController
@RequestMapping("/api/superadmin")
public class SuperAdminController {

    private final BranchService branchService;
    private final SuperAdminService superAdminService;

    public SuperAdminController(BranchService branchService, SuperAdminService superAdminService) {
        this.branchService = branchService;
        this.superAdminService = superAdminService;
    }

    // ----- Branches -----
    @GetMapping("/branches")
    public List<BranchDto> branches() {
        return branchService.list();
    }

    @PostMapping("/branches")
    public BranchDto createBranch(@RequestBody BranchRequest req) {
        return branchService.create(req);
    }

    @PutMapping("/branches/{id}")
    public BranchDto updateBranch(@PathVariable String id, @RequestBody BranchRequest req) {
        return branchService.update(id, req);
    }

    @PutMapping(value = "/branches/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BranchDto setBranchImage(@PathVariable String id,
                                    @RequestParam("kind") String kind,
                                    @RequestParam("image") MultipartFile image) {
        return branchService.setImage(id, kind, image);
    }

    @PostMapping("/branches/{id}/admins/{userId}")
    public void assignAdmin(@PathVariable String id, @PathVariable String userId) {
        branchService.assignAdmin(id, userId);
    }

    @PostMapping("/branches/{id}/stations/{stationId}")
    public void assignStation(@PathVariable String id, @PathVariable String stationId) {
        branchService.assignStation(id, stationId);
    }

    // ----- Admin oversight -----
    @GetMapping("/admins")
    public List<AdminSummaryDto> admins() {
        return superAdminService.listAdmins();
    }

    @PutMapping("/admins/{id}/branch")
    public AdminSummaryDto reassignBranch(@PathVariable String id,
                                          @RequestBody Map<String, String> body) {
        return superAdminService.reassignBranch(id, body.get("branchId"));
    }

    @PostMapping("/admins/{id}/disable")
    public AdminSummaryDto disableAdmin(@PathVariable String id) {
        return superAdminService.setDisabled(id, true);
    }

    @PostMapping("/admins/{id}/enable")
    public AdminSummaryDto enableAdmin(@PathVariable String id) {
        return superAdminService.setDisabled(id, false);
    }
}
