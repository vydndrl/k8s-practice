package beyond.ordersystem.product.controller;

import beyond.ordersystem.common.dto.CommonResDto;
import beyond.ordersystem.ordering.dto.ProductSearchDto;
import beyond.ordersystem.product.domain.Product;
import beyond.ordersystem.product.dto.ProductSaveReqDto;
import beyond.ordersystem.product.dto.ProductResDto;
import beyond.ordersystem.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ProductController {
    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/product/create")
    public ResponseEntity<?> createProduct( ProductSaveReqDto dto){
        Product product = productService.awsCreateProduct(dto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "product is successfully created", product), HttpStatus.CREATED);
    }

    @GetMapping("/product/list")
    public ResponseEntity<?> productList(ProductSearchDto searchDto, Pageable pageable) {
        Page<ProductResDto> products = productService.productList(searchDto, pageable);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "OK", products), HttpStatus.OK);
    }
}
