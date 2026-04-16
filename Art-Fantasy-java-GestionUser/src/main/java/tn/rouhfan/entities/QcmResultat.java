package tn.rouhfan.entities;

import java.time.LocalDateTime;

public class QcmResultat {
    private int id;
    private Qcm qcm;
    private User participant;
    private float score;
    private boolean valide;
    private LocalDateTime dateTentative;

    public QcmResultat() {
        this.dateTentative = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Qcm getQcm() { return qcm; }
    public void setQcm(Qcm qcm) { this.qcm = qcm; }

    public User getParticipant() { return participant; }
    public void setParticipant(User participant) { this.participant = participant; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    public boolean isValide() { return valide; }
    public void setValide(boolean valide) { this.valide = valide; }

    public LocalDateTime getDateTentative() { return dateTentative; }
    public void setDateTentative(LocalDateTime dateTentative) { this.dateTentative = dateTentative; }

    @Override
    public String toString() {
        return "QcmResultat{id=" + id + ", score=" + score + ", valide=" + valide + "}";
    }
}