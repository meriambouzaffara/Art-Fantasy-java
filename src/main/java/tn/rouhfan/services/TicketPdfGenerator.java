package tn.rouhfan.services;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import tn.rouhfan.entities.Evenement;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;

public class TicketPdfGenerator {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Génère un ticket PDF pour un participant à un événement.
     * @param event L'événement concerné.
     * @param participantName Le nom du participant.
     * @return Le fichier PDF généré.
     * @throws Exception En cas d'erreur de génération.
     */
    public File generateTicket(Evenement event, String participantName) throws Exception {
        String fileName = "Ticket_Evenement_" + event.getId() + "_" + System.currentTimeMillis() + ".pdf";
        File pdfFile = new File(fileName);
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        document.open();

        // Options de style
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, com.itextpdf.text.BaseColor.BLACK);
        Font fontSubTitle = FontFactory.getFont(FontFactory.HELVETICA, 16, com.itextpdf.text.BaseColor.DARK_GRAY);
        Font fontText = FontFactory.getFont(FontFactory.HELVETICA, 12, com.itextpdf.text.BaseColor.BLACK);

        // Ajout du Logo (si présent dans les ressources)
        try {
            URL logoUrl = getClass().getResource("/ui/logo.png");
            if (logoUrl != null) {
                com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(logoUrl);
                logo.scaleToFit(150, 150);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
            }
        } catch (Exception ex) {
            System.out.println("Logo non trouvé pour le PDF");
        }

        // Titre du Document
        Paragraph title = new Paragraph("TICKET DE PARTICIPATION", fontTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);
        document.add(title);

        // Infos Événement
        document.add(new Paragraph("Événement : " + event.getTitre(), fontSubTitle));
        document.add(new Paragraph("---------------------------------------------------------"));
        document.add(new Paragraph("Lieu : " + event.getLieu(), fontText));
        String dateFormatted = event.getDateEvent() != null ? dateFormat.format(event.getDateEvent()) : "Non définie";
        document.add(new Paragraph("Date : " + dateFormatted, fontText));
        document.add(new Paragraph("Type : " + event.getType(), fontText));

        document.add(new Paragraph(" ", fontText)); // Espace
        document.add(new Paragraph("Détails du Participant :", fontSubTitle));
        document.add(new Paragraph("---------------------------------------------------------"));
        document.add(new Paragraph("Nom du Participant : " + participantName, fontText));

        document.add(new Paragraph(" ", fontText));
        Paragraph footer = new Paragraph("Merci de votre participation ! Ce ticket est strictement personnel.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();

        return pdfFile;
    }
}
