package tn.rouhfan.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import tn.rouhfan.entities.Certificat;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.User;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

public class CertificatPdfGenerator {

    private static final BaseColor GOLD_COLOR = new BaseColor(212, 175, 55);
    private static final BaseColor DARK_BLUE = new BaseColor(44, 62, 80);
    private static final BaseColor LIGHT_GRAY = new BaseColor(128, 128, 128);
    private static final BaseColor DARK_GOLD = new BaseColor(184, 134, 11);

    /**
     * Génère un certificat PDF professionnel (tout sur une seule page)
     * @param certificat Le certificat à générer
     * @param filePath Chemin où sauvegarder le PDF
     * @throws Exception
     */
    public void generateCertificat(Certificat certificat, String filePath) throws Exception {
        // Format paysage A4
        Document document = new Document(PageSize.A4.rotate());
        document.setMargins(40, 40, 30, 30); // Réduire les marges
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // Bordures dorées avec double contour
        PdfContentByte canvas = writer.getDirectContent();

        // Premier contour doré épais
        canvas.setColorStroke(GOLD_COLOR);
        canvas.setLineWidth(12);
        canvas.rectangle(25, 25, document.getPageSize().getWidth() - 50, document.getPageSize().getHeight() - 50);
        canvas.stroke();

        // Deuxième contour doré fin
        canvas.setColorStroke(DARK_GOLD);
        canvas.setLineWidth(2);
        canvas.rectangle(35, 35, document.getPageSize().getWidth() - 70, document.getPageSize().getHeight() - 70);
        canvas.stroke();

        // Ornement en haut (plus petit)
        addTopOrnament(document);

        // En-tête
        addHeader(document);

        // Corps du certificat
        addBody(document, certificat);

        // Pied de page
        addFooter(document, certificat);

        document.close();
    }

    private void addTopOrnament(Document document) throws DocumentException {
        Font ornamentFont = new Font(Font.FontFamily.ZAPFDINGBATS, 16, Font.NORMAL, GOLD_COLOR);
        Paragraph ornament = new Paragraph("✦ ✦ ✦ ✦ ✦", ornamentFont);
        ornament.setAlignment(Element.ALIGN_CENTER);
        ornament.setSpacingBefore(10);
        ornament.setSpacingAfter(5);
        document.add(ornament);
    }

    private void addHeader(Document document) throws DocumentException {
        // Titre FÉLICITATIONS (plus petit)
        Font congratFont = new Font(Font.FontFamily.HELVETICA, 32, Font.BOLD | Font.ITALIC, DARK_BLUE);
        Paragraph felicitations = new Paragraph("FÉLICITATIONS !", congratFont);
        felicitations.setAlignment(Element.ALIGN_CENTER);
        felicitations.setSpacingBefore(10);
        felicitations.setSpacingAfter(5);
        document.add(felicitations);

        // Sous-titre
        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.ITALIC, LIGHT_GRAY);
        Paragraph subtitle = new Paragraph("Vous avez brillamment réussi votre formation", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(10);
        document.add(subtitle);

        // Ligne décorative
        addSeparator(document);

        // Titre du certificat
        Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 20, Font.BOLD | Font.UNDERLINE, DARK_GOLD);
        Paragraph title = new Paragraph("CERTIFICAT DE RÉUSSITE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(15);
        title.setSpacingAfter(10);
        document.add(title);
    }

    private void addSeparator(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(40);
        line.setHorizontalAlignment(Element.ALIGN_CENTER);
        line.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        line.getDefaultCell().setFixedHeight(1);
        line.getDefaultCell().setBackgroundColor(GOLD_COLOR);
        document.add(line);
    }

    private void addBody(Document document, Certificat certificat) throws DocumentException {
        Cours cours = certificat.getCours();
        User participant = certificat.getParticipant();

        Font nameFont = new Font(Font.FontFamily.TIMES_ROMAN, 32, Font.BOLD | Font.ITALIC, DARK_BLUE);
        Font textFont = new Font(Font.FontFamily.TIMES_ROMAN, 14, Font.NORMAL, DARK_BLUE);
        Font courseFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD | Font.ITALIC, DARK_GOLD);
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, LIGHT_GRAY);

        // Nom complet du participant
        String nomComplet = participant.getPrenom() + " " + participant.getNom().toUpperCase();
        Paragraph name = new Paragraph(nomComplet, nameFont);
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(10);
        name.setSpacingAfter(10);
        document.add(name);

        // Texte de validation
        Paragraph validationText = new Paragraph(
                "a validé avec succès l'intégralité du parcours pédagogique\net les examens du cours :",
                textFont
        );
        validationText.setAlignment(Element.ALIGN_CENTER);
        validationText.setSpacingBefore(15);
        validationText.setSpacingAfter(10);
        document.add(validationText);

        // Nom du cours
        Paragraph coursName = new Paragraph("\"" + cours.getNom() + "\"", courseFont);
        coursName.setAlignment(Element.ALIGN_CENTER);
        coursName.setSpacingBefore(5);
        coursName.setSpacingAfter(10);
        document.add(coursName);

        // Encadré des résultats
        addResultBox(document, certificat);

        // Artiste / Expert
        if (cours.getArtiste() != null) {
            User artiste = cours.getArtiste();
            Paragraph expert = new Paragraph(
                    "Formation dispensée par : " + artiste.getPrenom() + " " + artiste.getNom().toUpperCase(),
                    smallFont
            );
            expert.setAlignment(Element.ALIGN_CENTER);
            expert.setSpacingBefore(10);
            document.add(expert);
        }
    }

    private void addResultBox(Document document, Certificat certificat) throws DocumentException {
        PdfPTable resultBox = new PdfPTable(2);
        resultBox.setWidthPercentage(50);
        resultBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        resultBox.setSpacingBefore(10);
        resultBox.setSpacingAfter(5);

        resultBox.getDefaultCell().setBorder(PdfPCell.BOX);
        resultBox.getDefaultCell().setBorderColor(LIGHT_GRAY);
        resultBox.getDefaultCell().setPadding(8);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, DARK_BLUE);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, DARK_GOLD);

        // Score
        if (certificat.getScore() != null) {
            PdfPCell scoreLabel = new PdfPCell(new Paragraph("Score obtenu :", labelFont));
            scoreLabel.setBorder(PdfPCell.NO_BORDER);
            resultBox.addCell(scoreLabel);

            PdfPCell scoreValue = new PdfPCell(new Paragraph(certificat.getScore() + " %", valueFont));
            scoreValue.setBorder(PdfPCell.NO_BORDER);
            resultBox.addCell(scoreValue);
        }

        // Niveau
        if (certificat.getNiveau() != null) {
            PdfPCell niveauLabel = new PdfPCell(new Paragraph("Niveau :", labelFont));
            niveauLabel.setBorder(PdfPCell.NO_BORDER);
            resultBox.addCell(niveauLabel);

            PdfPCell niveauValue = new PdfPCell(new Paragraph(certificat.getNiveau(), valueFont));
            niveauValue.setBorder(PdfPCell.NO_BORDER);
            resultBox.addCell(niveauValue);
        }

        document.add(resultBox);
    }

    private void addFooter(Document document, Certificat certificat) throws DocumentException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String dateStr = sdf.format(certificat.getDateObtention());

        Font smallFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, LIGHT_GRAY);
        Font italicFont = new Font(Font.FontFamily.TIMES_ROMAN, 11, Font.ITALIC, DARK_BLUE);

        // Ligne de séparation
        addSeparator(document);

        // Date et numéro sur la même ligne
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(80);
        footerTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        footerTable.setSpacingBefore(10);

        footerTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        footerTable.getDefaultCell().setPadding(3);

        // Date
        PdfPCell dateCell = new PdfPCell(new Paragraph("Délivré le " + dateStr, smallFont));
        dateCell.setBorder(PdfPCell.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        footerTable.addCell(dateCell);

        // Numéro
        PdfPCell idCell = new PdfPCell(new Paragraph("Certificat n° " + certificat.getId() + "-" + certificat.getParticipant().getId(), smallFont));
        idCell.setBorder(PdfPCell.NO_BORDER);
        idCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footerTable.addCell(idCell);

        document.add(footerTable);

        // Signature
        Paragraph signature = new Paragraph("Direction Academy", italicFont);
        signature.setAlignment(Element.ALIGN_RIGHT);
        signature.setSpacingBefore(15);
        document.add(signature);

        // Petit sceau
        Font sealFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, GOLD_COLOR);
        Paragraph seal = new Paragraph("✧ SCEAU OFFICIEL ✧", sealFont);
        seal.setAlignment(Element.ALIGN_CENTER);
        seal.setSpacingBefore(5);
        document.add(seal);
    }
}