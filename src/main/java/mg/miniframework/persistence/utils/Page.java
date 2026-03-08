package mg.miniframework.persistence.utils;

import java.util.List;

/**
 * Classe générique pour la pagination des résultats
 */
public class Page<T> {
    private List<T> content;
    private int number; // numéro de page actuelle (1-indexed)
    private int size; // taille de la page
    private long totalElements; // nombre total d'éléments
    private int totalPages; // nombre total de pages

    public Page(List<T> content, int number, int size, long totalElements) {
        this.content = content;
        this.number = number;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.max(1, Math.ceil((double) totalElements / size));
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean hasNextPage() {
        return number < totalPages;
    }

    public boolean hasPreviousPage() {
        return number > 1;
    }

    // JavaBean-style boolean getters so JSP EL can access these properties as
    // `${page.hasNextPage}` / `${page.hasPreviousPage}`
    public boolean getHasNextPage() {
        return hasNextPage();
    }

    public boolean getHasPreviousPage() {
        return hasPreviousPage();
    }

    public int getNextPageNumber() {
        return hasNextPage() ? number + 1 : number;
    }

    public int getPreviousPageNumber() {
        return hasPreviousPage() ? number - 1 : 1;
    }

    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }
}
