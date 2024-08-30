package beyond.ordersystem.product.domain;

import beyond.ordersystem.product.dto.ProductResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private String imagePath;

    public void updateImagePath(String imagePath){
        this.imagePath = imagePath;
    }

    public ProductResDto fromEntity(){
        return ProductResDto.builder()
                .id(this.id)
                .category(this.category)
                .price(this.price)
                .stockQuantity(this.stockQuantity)
                .name(this.name)
                .imagePath(this.imagePath)
                .build();
    }

    public void updateStockQuantity(int quantity){
        this.stockQuantity = this.stockQuantity - quantity;
    }
}
