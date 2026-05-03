package tn.rouhfan.services;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.rouhfan.entities.Article;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PanierService {

    private static final PanierService INSTANCE = new PanierService();

    private final ObservableList<PanierItem> items = FXCollections.observableArrayList();
    private final IntegerProperty revision = new SimpleIntegerProperty(0);

    private PanierService() {
    }

    public static PanierService getInstance() {
        return INSTANCE;
    }

    public boolean addArticle(Article article) {
        if (article == null || article.getIdArticle() == null || !hasAvailableStock(article)) {
            return false;
        }

        PanierItem existing = findItem(article);
        if (existing != null) {
            int stock = article.getStock() != null ? article.getStock() : Integer.MAX_VALUE;
            if (existing.getQuantity() >= stock) {
                return false;
            }
            existing.increment();
            touch();
            return true;
        }

        items.add(new PanierItem(article, 1));
        touch();
        return true;
    }

    public void increment(PanierItem item) {
        if (item == null || item.getArticle() == null) {
            return;
        }
        int stock = item.getArticle().getStock() != null ? item.getArticle().getStock() : Integer.MAX_VALUE;
        if (item.getQuantity() < stock) {
            item.increment();
            touch();
        }
    }

    public void decrement(PanierItem item) {
        if (item == null) {
            return;
        }
        if (item.getQuantity() <= 1) {
            items.remove(item);
        } else {
            item.decrement();
        }
        touch();
    }

    public void remove(PanierItem item) {
        items.remove(item);
        touch();
    }

    public void clear() {
        items.clear();
        touch();
    }

    public ObservableList<PanierItem> getItems() {
        return FXCollections.unmodifiableObservableList(items);
    }

    public List<PanierItem> snapshot() {
        return new ArrayList<>(items);
    }

    public int getItemCount() {
        return items.stream().mapToInt(PanierItem::getQuantity).sum();
    }

    public double getTotal() {
        return items.stream().mapToDouble(PanierItem::getSubtotal).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public ReadOnlyIntegerProperty revisionProperty() {
        return revision;
    }

    private PanierItem findItem(Article article) {
        return items.stream()
                .filter(item -> item.getArticle() != null
                        && Objects.equals(item.getArticle().getIdArticle(), article.getIdArticle()))
                .findFirst()
                .orElse(null);
    }

    private boolean hasAvailableStock(Article article) {
        return article.getStock() == null || article.getStock() > 0;
    }

    private void touch() {
        revision.set(revision.get() + 1);
    }
}
