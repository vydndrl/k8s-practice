package beyond.ordersystem.member.domain;

import beyond.ordersystem.common.domain.Address;
import beyond.ordersystem.common.domain.BaseTimeEntity;
import beyond.ordersystem.member.dto.MemberResDto;
import beyond.ordersystem.ordering.domain.Ordering;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Min;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

//    @Column(columnDefinition = "ENUM('ADMIN', 'USER') DEFAULT 'USER'")
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Embedded
    private Address address;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Ordering> orderingList;

    public MemberResDto fromEntity(){
        return MemberResDto.builder()
                .id(this.id)
                .name(this.name)
                .email(this.email)
                .address(this.address)
                .orderCount(this.orderingList.size())
                .build();
    }
    public void updatePassword(String password){
        this.password = password;
    }

}
