module com.yathindra.downloadmanager {
    requires javafx.controls;
    requires javafx.fxml;
//    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.logging;
    requires xstream;
//    requires com.jfoenix;

    opens com.yathindra.downloadmanager to javafx.fxml;
    exports com.yathindra.downloadmanager;
}