package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.dto.response.KktsResponse;
import saigonbank.fit.ims.entity.Kkts;
import saigonbank.fit.ims.entity.Product;
import saigonbank.fit.ims.repository.KktsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KktsService {

    private final KktsRepository kktsRepository;

    @Transactional(readOnly = true)
    public List<KktsResponse> getKktsByPeriod(Integer year, Integer month) {
        List<Kkts> list = kktsRepository.findByPeriod(year, month);
        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private KktsResponse mapToResponse(Kkts k) {
        Product p = k.getProduct();
        return KktsResponse.builder()
                .id(k.getId())
                .productId(p.getId())
                .maSo(p.getMaSo())
                .tenHang(p.getTenHang())
                .nhomHang(p.getNhomHangCode())
                .donViTinh(p.getDonViTinh())
                .year(k.getYear())
                .month(k.getMonth())
                .openingQuantity(k.getOpeningQuantity())
                .openingAmount(k.getOpeningAmount())
                .importQuantity(k.getImportQuantity())
                .importAmount(k.getImportAmount())
                .exportQuantity(k.getExportQuantity())
                .exportAmount(k.getExportAmount())
                .endingQuantity(k.getEndingQuantity())
                .endingAmount(k.getEndingAmount())
                .averagePrice(k.getAveragePrice())
                .build();
    }
}
