package tn.rouhfan.entities;

import java.time.LocalDateTime;

public class CoursReview {

    private int           id;
    private Cours         cours;
    private User          participant;
    private int           note;        // 1 à 5 étoiles
    private LocalDateTime dateReview;

    public CoursReview() {
        this.dateReview = LocalDateTime.now();
    }

    public CoursReview(Cours cours, User participant, int note) {
        this.cours       = cours;
        this.participant = participant;
        this.note        = note;
        this.dateReview  = LocalDateTime.now();
    }

    public int getId()                         { return id; }
    public void setId(int id)                  { this.id = id; }

    public Cours getCours()                    { return cours; }
    public void setCours(Cours cours)          { this.cours = cours; }

    public User getParticipant()               { return participant; }
    public void setParticipant(User p)         { this.participant = p; }

    public int getNote()                       { return note; }
    public void setNote(int note)              { this.note = Math.max(1, Math.min(5, note)); }

    public LocalDateTime getDateReview()       { return dateReview; }
    public void setDateReview(LocalDateTime d) { this.dateReview = d; }

    /** "⭐⭐⭐☆☆" selon la note */
    public String getEtoilesDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= note ? "⭐" : "☆");
        return sb.toString();
    }
}
