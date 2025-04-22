package rita.artha.shastra.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rita.artha.shastra.dto.VehicleBrandDto;
import rita.artha.shastra.entity.VehicleBrand;
import rita.artha.shastra.entity.VehicleCategory;
import rita.artha.shastra.repository.VehicleBrandRepository;
import rita.artha.shastra.repository.VehicleCategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleBrandService {

    private final VehicleCategoryRepository vehicleCategoryRepository;
    private final VehicleBrandRepository vehicleBrandRepository;

    public List<VehicleCategory> getAllCategories() {
        return vehicleCategoryRepository.findAll();
    }

    public List<VehicleBrand> getBrandsByCategory(Integer categoryId) {
        return vehicleBrandRepository.findByCategoryId(categoryId);
    }

    public VehicleBrand createBrand(VehicleBrandDto dto) {
        VehicleCategory category = vehicleCategoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        VehicleBrand brand = new VehicleBrand();
        brand.setName(dto.getName());
        brand.setCategory(category);
        return vehicleBrandRepository.save(brand);
    }

    public VehicleBrand updateBrand(Integer id, VehicleBrandDto dto) {
        VehicleBrand existing = vehicleBrandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        VehicleCategory category = vehicleCategoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        existing.setName(dto.getName());
        existing.setCategory(category);
        return vehicleBrandRepository.save(existing);
    }

    public void deleteBrand(Integer id) {
        vehicleBrandRepository.deleteById(id);
    }
}