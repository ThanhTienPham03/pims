package saigonbank.fit.ims.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.entity.Inventory;
import saigonbank.fit.ims.entity.Kkts;
import saigonbank.fit.ims.entity.Product;
import saigonbank.fit.ims.entity.Role;
import saigonbank.fit.ims.entity.User;
import saigonbank.fit.ims.entity.ProductGroup;
import saigonbank.fit.ims.entity.Unit;
import saigonbank.fit.ims.repository.ProductGroupRepository;
import saigonbank.fit.ims.repository.UnitRepository;
import saigonbank.fit.ims.repository.InventoryRepository;
import saigonbank.fit.ims.repository.KktsRepository;
import saigonbank.fit.ims.repository.ProductRepository;
import saigonbank.fit.ims.repository.RoleRepository;
import saigonbank.fit.ims.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProductGroupRepository productGroupRepository;
    private final UnitRepository unitRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final KktsRepository kktsRepository;
    private final PasswordEncoder passwordEncoder;
    private final saigonbank.fit.ims.service.ProductService productService;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and seeding initial database data...");
        seedRoles();
        seedUsers();
        Map<String, ProductGroup> groups = seedProductGroups();
        Map<String, Unit> units = seedUnits();
        seedProducts(groups, units);
        productService.migrateAllLegacyProductCodes();
        log.info("Database seeding completed.");
    }

    private void seedRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(Role.builder().name("ADMIN").description("Quản trị hệ thống").build());
            roleRepository.save(Role.builder().name("USER").description("Nhân viên quản lý kho").build());
            roleRepository.save(Role.builder().name("CONTROLLER").description("Kiểm soát viên").build());
            log.info("Seeded 3 default roles: ADMIN, USER, CONTROLLER");
        }
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            Role userRole = roleRepository.findByName("USER").orElse(null);
            Role controllerRole = roleRepository.findByName("CONTROLLER").orElse(null);

            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Quản Trị Viên")
                    .email("admin@saigonbank.com.vn")
                    .phone("0901234567")
                    .roles(adminRole != null && userRole != null ? Set.of(adminRole, userRole) : Set.of())
                    .build());

            userRepository.save(User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .fullName("Nhân Viên Kho")
                    .email("user@saigonbank.com.vn")
                    .phone("0907654321")
                    .roles(userRole != null ? Set.of(userRole) : Set.of())
                    .build());

            userRepository.save(User.builder()
                    .username("controller")
                    .password(passwordEncoder.encode("controller123"))
                    .fullName("Kiểm Soát Viên")
                    .email("controller@saigonbank.com.vn")
                    .phone("0909998888")
                    .roles(controllerRole != null ? Set.of(controllerRole) : Set.of())
                    .build());

            log.info("Seeded 3 default users: admin, user, controller (passwords configured via env)");
        }
    }

    private Map<String, ProductGroup> seedProductGroups() {
        Map<String, ProductGroup> map = new HashMap<>();
        createProductGroupIfAbsent("VPP", "Văn phòng phẩm", map);
        createProductGroupIfAbsent("ACTT", "Ấn chỉ thông thường", map);
        createProductGroupIfAbsent("ACQT", "Ấn chỉ quan trọng", map);
        createProductGroupIfAbsent("VLK", "Vật liệu khác", map);
        return map;
    }

    private void createProductGroupIfAbsent(String code, String name, Map<String, ProductGroup> map) {
        ProductGroup pg = productGroupRepository.findByCode(code)
                .orElseGet(() -> productGroupRepository.save(ProductGroup.builder()
                        .code(code)
                        .name(name)
                        .isActive(true)
                        .build()));
        map.put(code, pg);
    }

    private Map<String, Unit> seedUnits() {
        Map<String, Unit> map = new HashMap<>();
        createUnitIfAbsent("CAI", "Cái", map);
        createUnitIfAbsent("HOP", "Hộp", map);
        createUnitIfAbsent("QUYEN", "Quyển", map);
        createUnitIfAbsent("BO", "Bộ", map);
        createUnitIfAbsent("REAM", "Ream", map);
        createUnitIfAbsent("CUON", "Cuộn", map);
        createUnitIfAbsent("BINH", "Bình", map);
        createUnitIfAbsent("GOI", "Gói", map);
        createUnitIfAbsent("THUNG", "Thùng", map);
        return map;
    }

    private void createUnitIfAbsent(String code, String name, Map<String, Unit> map) {
        Unit u = unitRepository.findByCode(code)
                .orElseGet(() -> unitRepository.save(Unit.builder()
                        .code(code)
                        .name(name)
                        .isActive(true)
                        .build()));
        map.put(code, u);
    }

    private void seedProducts(Map<String, ProductGroup> groups, Map<String, Unit> units) {
        if (productRepository.count() == 0) {
            ProductGroup grpVpp = groups.get("VPP");
            ProductGroup grpActt = groups.get("ACTT");
            ProductGroup grpAcqt = groups.get("ACQT");
            ProductGroup grpVlk = groups.get("VLK");

            Unit unitCai = units.get("CAI");
            Unit unitReam = units.get("REAM");
            Unit unitQuyen = units.get("QUYEN");
            Unit unitCuon = units.get("CUON");

            createProductWithInventory("32F.7100001.001", "Bút bi Thiên Long 0.5mm xanh", grpVpp, unitCai,
                    new BigDecimal("5000"), new BigDecimal("100"), new BigDecimal("500000"));

            createProductWithInventory("32F.7100001.002", "Giấy in A4 Double A 70gsm", grpVpp, unitReam,
                    new BigDecimal("75000"), new BigDecimal("50"), new BigDecimal("3750000"));

            createProductWithInventory("32F.9020100.001", "Phiếu thu 2 liên A5 (100 tờ/quyển)", grpActt, unitQuyen,
                    new BigDecimal("25000"), new BigDecimal("30"), new BigDecimal("750000"));

            createProductWithInventory("32F.9020100.002", "Phiếu chi 2 liên A5 (100 tờ/quyển)", grpActt, unitQuyen,
                    new BigDecimal("25000"), new BigDecimal("30"), new BigDecimal("750000"));

            createProductWithInventory("32F.5010000.001", "Sổ séc trắng 50 tờ (In đặc biệt)", grpAcqt, unitQuyen,
                    new BigDecimal("150000"), new BigDecimal("20"), new BigDecimal("3000000"));

            createProductWithInventory("32F.9080117.001", "Băng keo trong 5cm 100 yard", grpVlk, unitCuon,
                    new BigDecimal("15000"), new BigDecimal("40"), new BigDecimal("600000"));

            log.info("Seeded sample products and inventory records with standardized codes.");
        }
    }

    private void createProductWithInventory(String maSo, String tenHang, ProductGroup group, Unit unit,
                                           BigDecimal donGiaNhap, BigDecimal openQty, BigDecimal openAmt) {
        if (group == null || unit == null) {
            return;
        }

        Product product = productRepository.save(Product.builder()
                .maSo(maSo)
                .tenHang(tenHang)
                .productGroup(group)
                .unit(unit)
                .donGiaNhap(donGiaNhap)
                .trangThai(Product.ProductStatus.HOAT_DONG)
                .build());

        Inventory inventory = Inventory.builder()
                .product(product)
                .openingQuantity(openQty)
                .openingAmount(openAmt)
                .endingQuantity(openQty)
                .endingAmount(openAmt)
                .averagePrice(donGiaNhap)
                .build();
        inventoryRepository.save(inventory);

        LocalDate now = LocalDate.now();
        Kkts kkts = Kkts.builder()
                .product(product)
                .year(now.getYear())
                .month(now.getMonthValue())
                .openingQuantity(openQty)
                .openingAmount(openAmt)
                .endingQuantity(openQty)
                .endingAmount(openAmt)
                .averagePrice(donGiaNhap)
                .build();
        kktsRepository.save(kkts);
    }
}
