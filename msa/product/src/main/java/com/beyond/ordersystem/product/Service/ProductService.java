package com.beyond.ordersystem.product.Service;
import com.beyond.ordersystem.common.service.StockInventoryService;
//import com.beyond.ordersystem.product.Controller.ProductController;
import com.beyond.ordersystem.product.Repository.ProductRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.dto.ProductResDto;
import com.beyond.ordersystem.product.dto.ProductSaveReqDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.dto.ProductUpdateStockDto;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
//import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
//import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final S3Client s3Client;
    private final StockInventoryService stockInventoryService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Autowired
    public ProductService(ProductRepository productRepository, S3Client s3Client, StockInventoryService stockInventoryService){
        this.productRepository = productRepository;
        this.s3Client = s3Client;
        this.stockInventoryService = stockInventoryService;
    }

    public Product createProduct(ProductSaveReqDto dto) {
        MultipartFile image = dto.getProductImage();
        Product product = null;
        try {
            product = productRepository.save(dto.toEntity());
            byte[] bytes = image.getBytes();
            Path path = Paths.get("/tmp/",
                    product.getId() + "_" + image.getOriginalFilename());
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            product.updateImagePath(path.toString());   // 더티 체킹. 변경 감기

            if(dto.getName().contains("sale")){
                stockInventoryService.increaseStock(product.getId(), dto.getStockQuantity());
            }

        } catch (IOException e) {
            // 예외를 터뜨려줘야지 잡아버리면 안됨
            throw new RuntimeException("이미지 저장 실패");
        }
        return product;
    }
    public Product awsCreateProduct(ProductSaveReqDto dto) {
        MultipartFile image = dto.getProductImage();
        Product product = null;

        try {
            product = productRepository.save(dto.toEntity());
            byte[] bytes = image.getBytes();
            String fileName = product.getId() + "_" + image.getOriginalFilename();
            Path path = Paths.get("/tmp/", fileName);
            // local pc에 임시 저장
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

            // aws에 pc에 저장된 파일을 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();
            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, RequestBody.fromFile(path));
            String s3Path = s3Client.utilities().getUrl(a->a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImagePath(s3Path);   // 더티 체킹. 변경 감기
        } catch (IOException e) {
            // 예외를 터뜨려줘야지 잡아버리면 안됨
            throw new RuntimeException("이미지 저장 실패");
        }
        return product;

    }


    public Page<ProductResDto> productList(ProductSearchDto searchDto, Pageable pageable){
        // 검색을 위해 Specification 객체 사용
        // Specification 객체는 복잡한 쿼리를 명세를 이용하여 정의하는 방식으로, 쿼리를 쉽게 생성
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                if(searchDto.getSearchName() != null){
                    // DB 컬럼 조회 : root - 엔티티의 속성 조회
                    // root : 엔티티의 속성을 접근하기 위한 객체, CriteriaBuilder는 쿼리를 생성하기 위한 객체
                    // select * from product where name like '%hong%';
                    predicates.add(criteriaBuilder.like(root.get("name"), "%"+searchDto.getSearchName()+"%"));
                }
                if(searchDto.getCategory() != null){
                    predicates.add(criteriaBuilder.like(root.get("category"), "%"+searchDto.getCategory()+"%"));
                }
                // where name like '%사과%' and category like '%fruit%';
                Predicate[] predicatesArr = new Predicate[predicates.size()];
                for(int i=0; i<predicatesArr.length; i++){
                    predicatesArr[i] = predicates.get(i);
                }
                // 위 2개의 쿼리 조건문을 and 조건으로 연결
                Predicate predicate = criteriaBuilder.and(predicatesArr);
                return predicate;
            }
        };

        Page<Product> products = productRepository.findAll(specification, pageable);
        Page<ProductResDto> productResDtos = products.map(a->a.fromEntity());
        return productResDtos;
    }

    public ProductResDto productDetail(Long id){
        return productRepository.findById(id).orElseThrow(()->new EntityNotFoundException("존재하지 않는 id입니다.")).fromEntity();
    }


    public Product productUpdateStock(ProductUpdateStockDto dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));
        product.updateStockQuantity(dto.getProductQuantity());
        return product;
    }

//    @KafkaListener(topics = "product-update-topic", groupId = "order-group", containerFactory = "kafkaListenerContainerFactory")
//    public void consumerProductQuantity(String message) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            ProductUpdateStockDto productUpdateStockDto
//                    = objectMapper.readValue(message, ProductUpdateStockDto.class);
//            System.out.println(productUpdateStockDto);
//            this.productUpdateStock(productUpdateStockDto);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//
//    }

}
