package com.ducnhu.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_review_product_time", columnList = "product_id, reviewTime"),
                @Index(name = "idx_review_customer_product", columnList = "customer_id, product_id"),
                @Index(name = "idx_review_votes", columnList = "votes")
        }
)
@Builder
@Getter
@Setter
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 126)
    private String headLine;

    @Column(length = 300, nullable = false)
    private String comment;

    private int rating;

    private int votes;

    @Transient
    private boolean upVotedByCurrentCustomer;

    @Transient
    private boolean downVotedByCurrentCustomer;

    @Column(nullable = false)
    private Date reviewTime;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "customer_id")
    private Integer customerId;

    public Review() {
    }
}
