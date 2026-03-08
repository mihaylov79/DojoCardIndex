package cardindex.dojocardindex.Document.model;

public enum DocumentCategory {

    REGULATION("Наредба"),
    PROGRAM("Програма"),
    DECLARATION("Декларация"),
    FORM("Формуляр"),
    ANNOUNCEMENT("Обявление"),
    REPORT("Доклад"),
    PRICE_LIST("Ценоразпис"),
    INFO("Инфо"),
    PRINCIPLES("Принципи"),
    OTHER("Други");

    private final String description;

    DocumentCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
