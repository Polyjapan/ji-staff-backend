package tools

import models.FormModel.{BooleanDecorator, ChoiceListDecorator, DateDecorator, Form, FormEntry, FormModel, IntegerDecorator, StringDecorator}

import scala.concurrent.ExecutionContext

/**
  * An object that allows you to create the two basic forms
  * @author Louis Vialar
  */
object TemporaryForms {
  lazy val buildApplicationForm: Form = Form("application", "Candidature Staff", Map[String, FormEntry]()
    .updated("tshirt-size", FormEntry("Taille de T-Shirt", "", ordering = 0, decorator = ChoiceListDecorator(Set("xs", "s", "m", "l", "xl", "xxl", "xxxl"))))
    .updated("food", FormEntry("Régime alimentaire", "Précise ici si tu as un régime alimentaire particulier ou des allergies", ordering = 1, required = false, decorator = StringDecorator("^(\\p{L}||\\p{P}|||\\p{Zs}||[0-9])+$")))
    .updated("sleep", FormEntry("Dodo", "Souhaites-tu dormir sur place dans les abris P.C. de l'EPFL lors de la convention et/ou du montage ?", ordering = 2, decorator = BooleanDecorator()))
    .updated("particular-help", FormEntry("Aide particulière", "Pour les besoins de l'installation, certaines tâches particulières seront attribuées à des staffs spécifiques. Ces tâches incluent entre autres l'installation électrique, la gestion des installations murales, etc... Si tu es intéressé et disponible durant la semaine précédant la convention, donne-nous tes horaires. Ceci implique que tu seras aussi responsable de cette tâche durant le démontage. ATTENTION, répondre à cette question ne t'engage pas définitivement. Cela veut juste dire que nous te proposerons peut-être un tel poste que tu seras ensuite libre d'accepter ou refuser.", decorator = ChoiceListDecorator(Set("Je suis intéressé et disponible pour venir aider quelques jours avant la convention", "Je suis intéressé mais ne suis pas disponible durant la semaine précédant la convention")), ordering = 3))
    .updated("previous-staff", FormEntry("Combien de fois as tu fais staff JI ?", "", ordering = 4, decorator = IntegerDecorator(0, 9, 1)))
    .updated("english-level", FormEntry("Niveau d'anglais", "Selon le CEFR/CEF (0 = rien, 6 = C2/Langue maternelle)", ordering = 5, decorator = IntegerDecorator(0, 6, 1)))
    .updated("german-level", FormEntry("Niveau d'allemand", "Selon le CEFR/CEF (0 = rien, 6 = C2/Langue maternelle)", ordering = 6, decorator = IntegerDecorator(0, 6, 1)))
    .updated("japanese-level", FormEntry("Niveau de japonais", "Selon le JPLT (0 = rien, 5 = JPLT N1)", ordering = 7, decorator = IntegerDecorator(0, 5, 1)))
    .updated("job", FormEntry("Travail", "Ou plus généralement : \"Tu fais quoi dans la vie?\"", ordering = 8, decorator = StringDecorator("^(\\p{L}||\\p{P}|||\\p{Zs}||[0-9])+$")))
    .updated("skills", FormEntry("Compétences particulières", "Tout ce qui pourraît nous servir !", ordering = 9, required = false, decorator = StringDecorator("^(\\p{L}||\\p{P}|||\\p{Zs}||[0-9])+$")))
    .updated("motivation", FormEntry("Motivation", "Un petit paragraphe pour nous dire pourquoi tu veux être staff", ordering =10, decorator = StringDecorator("^(\\p{L}||\\p{P}|||\\p{Zs}||[0-9])+$")))
    .updated("prefered-job", FormEntry("Préférence de poste", "Quel poste préfèrerais tu exercer, et pourquoi ?", ordering = 11, required = false, decorator = StringDecorator("^(\\p{L}||\\p{P}|||\\p{Zs}||[0-9])+$")))
    .updated("conditions", FormEntry("J'ai lu et approuvé les conditions", "Eh oui, être staff c'est avoir droit à pas mal de choses dont:\n-L'entrée gratuite dans la convention.\n-Deux entrées dimanche gratuites à offrir aux personnes de ton choix.\n-Le T-shirt officiel de staff Japan-Impact.\n-Les repas offerts durant la convention.\n-Un espace où stocker ses affaires et se reposer.\n-La possibilité de dormir sur place.\n-Une soirée de remerciement aux staffs tenue plus tard dans l'année\n-La possibilité de découvrir une convention de l'intérieur et possiblement d'acquérir quelques connaissances intéressantes.\n-Au moins une moitié de la journée en temps libre voire plus (dépendant du nombre de staffs) durant la convention pour en profiter.\n\nLes conditions obligatoires pour être staff sont les suivantes:\n-Être présent sur les lieux de la convention à partir de 08:00 jusqu'à 22:00 durant le week-end de déroulement les 17 et 18 février.\n-Être présent le vendredi 16 février pour l'installation dès 17:00\n-Être présent à l'une des journées de formation staffs\n-Fournir une autorisation signée de son représentant légal si mineur.\n\nSi tu es motivé à nous aider, tu peux aussi nous prêter assistance pour certaines tâches dont:\n-Faire de la pub et de l'affichage pour Japan Impact.\n-Essayer de recruter des staffs supplémentaires parmi tes amis.\n-Participer aux séances de préparation de décorations.\n-Aider au montage durant la semaine précédant Japan Impact", ordering = 12, decorator = BooleanDecorator()))
    .updated("staffing-used-to-be-better", FormEntry("Staff c'était mieux avant", "", ordering = 13, decorator = ChoiceListDecorator(Set("Oui", "Bien sûr", "Of course", "Tellement", "Le Jeu", "Julien est un troll"))))
    .updated("remarks", FormEntry("Remarques", "Un truc de plus à nous dire ?", ordering = 14, required = false, decorator = StringDecorator("^(\\p{L}||\\p{P}|||\\p{Zs}||[0-9])+$")))
  )

  val strRegex = "^(\\p{L}||\\p{Pd}||\\p{Zs}){2,30}$"

  // http://emailregex.com/
  val mailRegex = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"

  lazy val buildProfileForm: Form = Form("profile", "Modifier votre profil", Map[String, FormEntry]()
    .updated("lastname", FormEntry("Nom", "", ordering = 0, decorator = StringDecorator(strRegex)))
    .updated("firstname", FormEntry("Prénom", "", ordering = 1, decorator = StringDecorator(strRegex)))
    .updated("birthdate", FormEntry("Date de naissance", "", ordering = 2, decorator = DateDecorator()))
    .updated("cellphone", FormEntry("Numéro de téléphone portable", "Préciser l'extension si ce n'est pas un numéro suisse. Si tu n'as pas de numéro de téléphone, indique 000 000 00 00", ordering = 3, decorator = StringDecorator("^[()0-9 +]{9,20}$")))
    .updated("phone", FormEntry("Numéro de téléphone fixe", "", ordering = 4, required = false, decorator = StringDecorator("^[()0-9 +]{9,20}$")))
    .updated("address", FormEntry("Addresse postale", "Rue. numéro, ville, code postal, état", ordering = 5, decorator = StringDecorator("^(\\p{L}||\\p{P}|||\\p{Zs}||[0-9])+$")))
    .updated("legal-lastname", FormEntry("Nom du représentant légal", "", ordering = 6, required = false, onlyIfMinor = true, decorator = StringDecorator(strRegex)))
    .updated("legal-firstname", FormEntry("Prénom du représentant légal", "", ordering = 7, required = false, onlyIfMinor = true, decorator = StringDecorator(strRegex)))
    .updated("legal-cellphone", FormEntry("Numéro de téléphone du responsable légal", "Préciser l'extension si ce n'est pas un numéro suisse.", ordering = 8, required = false, onlyIfMinor = true, decorator = StringDecorator("^[()0-9 +]{9,20}$")))
    .updated("legal-address", FormEntry("Addresse postale du responsable légal", "Rue. numéro, ville, code postal, état", ordering = 9, required = false, onlyIfMinor = true, decorator = StringDecorator("^(\\p{L}||\\p{P}|||\\p{Zs}||[0-9])+$")))
    .updated("legal-email", FormEntry("Addresse e-mail du responsable légal", "", ordering = 10, required = false, onlyIfMinor = true, decorator = StringDecorator(mailRegex)))
  )

  def createForms(model: FormModel)(implicit ec: ExecutionContext): Unit = {
    /*
Disponibilités dates
   */
    println(model.setForm(buildApplicationForm).value)
    println(model.setForm(buildProfileForm).value)


  }
}
