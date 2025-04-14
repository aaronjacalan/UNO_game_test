module com.example.uno.game.test.uno_game_test {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.uno.game.test.uno_game_test to javafx.fxml;
    exports com.example.uno.game.test.uno_game_test;
}