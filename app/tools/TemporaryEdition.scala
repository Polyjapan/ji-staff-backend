package tools

import java.util.Date

import data.Edition
import data.forms._
import models.EditionsModel
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

/**
  * An object that allows you to create the two basic forms
  *
  * @author Louis Vialar
  */
object TemporaryEdition {
  val pages: List[FormPage] = {
    val strRegex = "^(\\p{L}||\\p{Pd}||\\p{Zs}){2,30}$"
    val phoneRegex = "^[()0-9 +]{9,20}$"
    val addressRegex = "^(\\p{L}||\\p{P}|||\\p{Zs}||[0-9])+$"

    // http://emailregex.com/
    val mailRegex = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"

    val first = FormPage(0, minorOnly = false, "Informations personnelles", List(
      FormField("lastname", "Nom", order = 0).withValidator(StringRegexValidator("Nom invalide", strRegex)),
      FormField("firstname", "Prénom", order = 1).withValidator(StringRegexValidator("Prénom invalide", strRegex)),
      FormField("birthdate", "Date de naissance", order = 2).withValidator(DateValidator("Date de naissance invalide")),

      FormField("cellphone", "Numéro de téléphone portable", "Préciser l'extension si ce n'est pas un numéro suisse. Si tu n'as pas de numéro de téléphone, indique 000 000 00 00",
        order = 3).withValidator(StringRegexValidator("Numéro de portable invalide", phoneRegex)),
      FormField("phone", "Numéro de téléphone fixe", "Préciser l'extension si ce n'est pas un numéro suisse.",
        order = 4, required = false).withValidator(StringRegexValidator("Numéro fixe invalide", phoneRegex)),
        FormField("address", "Addresse postale", "Rue. numéro, ville, code postal, état", order = 5).withValidator(StringRegexValidator("Adresse invalide", addressRegex))
    ))

    val legal = FormPage(1, minorOnly = true, "Responsable légal", List(
      FormField("legal-lastname", "Nom du représentant légal", order = 0).withValidator(StringRegexValidator("Nom du responsable légal invalide", strRegex)),
      FormField("legal-firstname", "Prénom du représentant légal", order = 1).withValidator(StringRegexValidator("Prénom du responsable légal invalide", strRegex)),
      FormField("legal-cellphone", "Numéro de téléphone du responsable légal", "Préciser l'extension si ce n'est pas un numéro suisse.", order = 2).withValidator(StringRegexValidator("Numéro de téléphone du responsable légal invalide", phoneRegex)),
      FormField("legal-address", "Addresse postale du responsable légal", "Rue. numéro, ville, code postal, état", order = 3).withValidator(StringRegexValidator("Adresse du responsable légal invalide", addressRegex)),
      FormField("legal-email", "Addresse e-mail du responsable légal", controlType = "email", order = 4).withValidator(StringRegexValidator("E-Mail du responsable légal invalide", mailRegex))
    ))

    val sizes = List("XS", "S", "M", "L", "XL", "XXL", "XXXL")
    val yesNo = Set("Oui", "Non")
    val particularHelp = Set("Je suis intéressé et disponible pour venir aider quelques jours avant la convention", "Je suis intéressé mais ne suis pas disponible durant la semaine précédant la convention")

    val orga = FormPage(2, minorOnly = false, "Organisation", List(
      FormField("tshirt-size", "Taille de T-Shirt", order = 0, controlType = "select", additionalData = Json.obj("values" -> sizes)).withValidator(SetContainedValidator("Taille de T-Shirt invalide", sizes.toSet)),
        FormField("food", "Régime alimentaire", "Précise ici si tu as un régime alimentaire particulier ou des allergies", order = 1, required = false).withValidator(StringRegexValidator("Le régime alimentaire contient des caractères interdits", addressRegex)),
        FormField("sleep", "Dodo", "Souhaites-tu dormir sur place dans les abris P.C. de l'EPFL lors de la convention et/ou du montage ?", order = 2, controlType = "option", additionalData = Json.obj("values" -> yesNo)).withValidator(SetContainedValidator("Dodo invalide", yesNo)),
        FormField("particular-help", "Aide particulière", "Pour les besoins de l'installation, certaines tâches particulières seront attribuées à des staffs spécifiques. Ces tâches incluent entre autres l'installation électrique, la gestion des installations murales, etc... Si tu es intéressé et disponible durant la semaine précédant la convention, donne-nous tes horaires. Ceci implique que tu seras aussi responsable de cette tâche durant le démontage. ATTENTION, répondre à cette question ne t'engage pas définitivement. Cela veut juste dire que nous te proposerons peut-être un tel poste que tu seras ensuite libre d'accepter ou refuser.",
          order = 3, controlType = "option", additionalData = Json.obj("values" -> particularHelp)).withValidator(SetContainedValidator("Aide particulière invalide", particularHelp))
    ))

    val cefr = List("Aucune expérience", "A1", "A2", "B1", "B2", "C1", "C2", "Langue maternelle")
    val jplt = List("Aucune expérience", "N5", "N4", "N3", "N2", "N1", "Langue maternelle")

    val skills = FormPage(3, minorOnly = false, "Compétences", List(
      FormField("previous-staff", "Combien de fois as tu fais staff JI ?", order = 0, controlType = "number").withValidator(IntegerValueValidator("Vous êtes certain.e d'avoir fait staff plus de 9 fois ?", 0, 9)),
        FormField("english-level", "Niveau d'anglais", "Selon le CEFR/CEF", order = 1, controlType = "select", additionalData = Json.obj("values" -> cefr)).withValidator(SetContainedValidator("Anglais invalide", cefr.toSet)),
        FormField("german-level", "Niveau d'allemand", "Selon le CEFR/CEF", order = 2, controlType = "select", additionalData = Json.obj("values" -> cefr)).withValidator(SetContainedValidator("Allemand invalide", cefr.toSet)),
        FormField("japanese-level", "Niveau de japonais", "Selon le JPLT", order = 3, controlType = "select", additionalData = Json.obj("values" -> jplt)).withValidator(SetContainedValidator("Japonais invalide", jplt.toSet)),
        FormField("job", "Travail", "Ou plus généralement : \"Tu fais quoi dans la vie?\"", order = 4).withValidator(StringRegexValidator("Le travail contient des caractères interdits", addressRegex)),
        FormField("skills", "Compétences particulières", "Tout ce qui pourraît nous servir !", order = 5).withValidator(StringRegexValidator("Les compétences contiennent des caractères interdits", addressRegex))
    ))

    val staffBetter = Set("Oui", "Bien sûr", "Of course", "Tellement", "Le Jeu", "Julien est un troll")


    val motivations = FormPage(4, minorOnly = false, "Motivation & Remarques", List(
      FormField("motivation", "Motivation", "Un petit paragraphe pour nous dire pourquoi tu veux être staff", order = 0, controlType = "textarea"),
        FormField("prefered-job", "Préférence de poste", "Quel poste préfèrerais tu exercer, et pourquoi ?", order = 1, required = false, controlType = "textarea"),
        FormField("remarks", "Remarques", "Un truc de plus à nous dire ?", order = 2, required = false, controlType = "textarea"),
          FormField("staffing-used-to-be-better", "Staff c'était mieux avant", order = 3, controlType = "select", additionalData = Json.obj("values" -> staffBetter)).withValidator(SetContainedValidator("Non mais t'es sérieux ?", staffBetter))
    ))

    List(first, legal, orga, skills, motivations)
  }

  def createEditions(model: EditionsModel)(implicit ec: ExecutionContext): Unit = {
    /*
Disponibilités dates
   */
    val testStart = 1511283180000L
    val testEnd = 1511373599000L
    val officialStart = 1511373600000L
    val officialEnd = 1514761200000L
    var conventionStart = 1518822000000L

    model.setEdition(Edition("2017", new Date(testStart), new Date(testEnd), new Date(conventionStart), pages))
    model.setEdition(Edition("2018", new Date(officialStart), new Date(officialEnd), new Date(conventionStart), pages))
  }
}
