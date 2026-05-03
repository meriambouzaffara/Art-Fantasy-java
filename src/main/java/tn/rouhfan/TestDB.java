package tn.rouhfan;

import tn.rouhfan.services.EvenementService;

public class TestDB {
    public static void main(String[] args) {
        try {
            System.out.println("Testing EvenementService initialization...");
            EvenementService service = new EvenementService();
            System.out.println("Trying to fetch events...");
            System.out.println("Count: " + service.recuperer().size());
            System.out.println("SUCCESS!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
