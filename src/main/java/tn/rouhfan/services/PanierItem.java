package tn.rouhfan.services;

import tn.rouhfan.entities.Article;

public class PanierItem {

    private final Article article;
    private int quantity;

    public PanierItem(Article article, int quantity) {
        this.article = article;
        this.quantity = quantity;
    }

    public Article getArticle() {
        return article;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
    }

    public void increment() {
        quantity++;
    }

    public void decrement() {
        if (quantity > 1) {
            quantity--;
        }
    }

    public double getSubtotal() {
        return article != null ? article.getPrix() * quantity : 0;
    }
}
