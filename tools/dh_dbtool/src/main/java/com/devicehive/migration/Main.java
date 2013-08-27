package com.devicehive.migration;

public class Main {



    public static void main(String... args) {
        DatabaseUpdater updater = new DatabaseUpdater();
        updater.execute(System.out, System.err, args);
    }


}
