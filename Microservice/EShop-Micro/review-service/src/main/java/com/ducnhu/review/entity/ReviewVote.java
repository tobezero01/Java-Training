package com.ducnhu.review.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "reviews_votes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vote_review_customer", columnNames = {"review_id", "customer_id"})
        },
        indexes = {
                @Index(name = "idx_vote_review", columnList = "review_id"),
                @Index(name = "idx_vote_customer", columnList = "customer_id")
        }
)
public class ReviewVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private static final int VOTE_UP_POINT = 1;
    private static final int VOTE_DOWN_POINT = -1;

    private int votes;

    @Column(name = "customer_id")
    private Integer customerId;

    @ManyToOne
    @JoinColumn(name = "review_id")
    private Review review;

    public ReviewVote() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }


    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public void voteUp() {
        this.votes = VOTE_UP_POINT;
    }

    public void voteDown() {
        this.votes = VOTE_DOWN_POINT;
    }

    @Transient
    public boolean isVoteUp() {
        return this.votes == VOTE_UP_POINT;
    }

    @Transient
    public boolean isVoteDown() {
        return this.votes == VOTE_DOWN_POINT;
    }
}
