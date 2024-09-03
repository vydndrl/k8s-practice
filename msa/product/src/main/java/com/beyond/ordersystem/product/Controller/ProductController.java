package com.beyond.ordersystem.product.Controller;

import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.product.Service.ProductService;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.dto.ProductResDto;
import com.beyond.ordersystem.product.dto.ProductSaveReqDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.dto.ProductUpdateStockDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 해당 어노테이션 사용 시 아래 스프링 빈은 실시간 config 변경 사항의 대상이 된다.
@RestController
//@RequestMapping("/ordersystem/product")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService){
        this.productService = productService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("product/create")
    // 둘다 json으로 받고 싶으면 @RequestPart ProductSaveReqDto dto, @RequestPart MultipartFile productImage
    public ResponseEntity<Object> registerProduct(@ModelAttribute ProductSaveReqDto dto){ // multipart form data형식으로 받음
        Product product = productService.awsCreateProduct(dto);
        // body에 들어가는 HttpStatus상태
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "product등록 성공", product.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED); //header에 들어가는 상태
    }

    @GetMapping("product/list")
    public ResponseEntity<Object> productList(ProductSearchDto searchDto, Pageable pageable){
        Page<ProductResDto> dtos = productService.productList(searchDto, pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "상품리스트 정상조회 완료",dtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<?> productDetail(@PathVariable Long id){
        ProductResDto dto = productService.productDetail(id);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "상품리스트 정상조회 완료",dto);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    @PutMapping("/product/updatestock")
    public ResponseEntity<?> productStockUpdate(@RequestBody ProductUpdateStockDto dto) {

        Product product = productService.productUpdateStock(dto);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "Success", product.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }



}
