package tn.rouhfan.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import tn.rouhfan.entities.Evenement;

import java.io.InputStream;
import java.util.Collections;
import java.util.TimeZone;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Rouh Events Calendar Integration";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "/google-calendar-key.json";
    
    // Identifiant de l'agenda cible
    // "primary" signifie le calendrier par défaut du compte de service, 
    // l'ajouter à un vrai UI / dashboard nécessite un partage d'agenda si on utilise "primary".
    // Ou bien on peut préciser l'ID d'un Agenda public partagé. L'utilisateur semble vouloir "primary" (son agenda) implicitement.
    private static final String CALENDAR_ID = "benamarahiba41@gmail.com";

    private Calendar service;

    public GoogleCalendarService() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            
            // On charge le fichier JSON qui est dans "src/main/resources".
            InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null) {
                System.err.println("❌ Fichier de credentials " + CREDENTIALS_FILE_PATH + " non trouvé.");
                return;
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            this.service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
                    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Crée un événement dans Google Calendar et retourne son ID.
     */
    public String ajouterEvenement(Evenement eventBase) {
        if (service == null) return null;
        
        try {
            Event event = mapperEvenementGoogle(eventBase);
            Event createdEvent = service.events().insert(CALENDAR_ID, event).execute();
            System.out.println("✅ Événement ajouté sur Google Calendar: " + createdEvent.getHtmlLink());
            return createdEvent.getId();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ajout dans Google Calendar: " + e.getMessage());
            return null;
        }
    }

    /**
     * Modifie un événement existant.
     */
    public void modifierEvenement(String googleEventId, Evenement eventBase) {
        if (service == null || googleEventId == null || googleEventId.isEmpty()) return;

        try {
            Event event = mapperEvenementGoogle(eventBase);
            service.events().update(CALENDAR_ID, googleEventId, event).execute();
            System.out.println("✅ Événement modifié sur Google Calendar (" + googleEventId + ")");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la modification sur Google Calendar: " + e.getMessage());
        }
    }

    /**
     * Supprime un événement existant.
     */
    public void supprimerEvenement(String googleEventId) {
        if (service == null || googleEventId == null || googleEventId.isEmpty()) return;

        try {
            service.events().delete(CALENDAR_ID, googleEventId).execute();
            System.out.println("✅ Événement supprimé de Google Calendar (" + googleEventId + ")");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la suppression sur Google Calendar: " + e.getMessage());
        }
    }

    /**
     * Map notre objet Evenement (Entité) vers l'objet Event de l'API Google
     */
    private Event mapperEvenementGoogle(Evenement e) {
        Event event = new Event()
            .setSummary(e.getTitre())
            .setLocation(e.getLieu())
            .setDescription(e.getDescription() + "\n\nType: " + e.getType());

        // La timezone par défaut
        String timeZone = TimeZone.getDefault().getID();

        // Si la date est renseignée (Date sql), on créé EventDateTime
        if (e.getDateEvent() != null) {
            // Par défaut l'événement durera 2 heures depuis sa date début
            java.util.Date startDate = e.getDateEvent();
            java.util.Date endDate = new java.util.Date(startDate.getTime() + (2 * 60 * 60 * 1000)); // +2 heures

            DateTime startDateTime = new DateTime(startDate, TimeZone.getDefault());
            EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(timeZone);
            event.setStart(start);

            DateTime endDateTime = new DateTime(endDate, TimeZone.getDefault());
            EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(timeZone);
            event.setEnd(end);
        } else {
            // S'il n'y a pas de date, on met la date actuelle par défaut pour éviter des erreurs API
             DateTime today = new DateTime(new java.util.Date(), TimeZone.getDefault());
             EventDateTime d = new EventDateTime().setDateTime(today).setTimeZone(timeZone);
             event.setStart(d);
             event.setEnd(d);
        }

        return event;
    }
}
