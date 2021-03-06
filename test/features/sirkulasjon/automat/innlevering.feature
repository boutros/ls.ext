# encoding: UTF-8
# language: no

@issue-3
@wip
Egenskap: Selvbetjent innlevering
  Som en låner Knut
  Fordi jeg har sett filmen jeg lånte
  Ønsker jeg å levere den tilbake til biblioteket

  Notat: Disse scenariene tester ikke egentlig automaten, men at dersom vi oppfører oss
  som en automat så svarer Koha på en forutsigelig måte som passer for våre automater
  etter at vi har lastet dem med riktig oppsett. Test med automat er manuell.

  Bakgrunn:
    Gitt at det finnes en utlånsautomat
    Og at det finnes en låner med lånekort
      | firstname | dateenrolled | dateexpiry | password |
      | Knut      | 01/08/2015   | 01/01/2020 | 1234     |
    Og at låneren ikke har aktiv innkrevingssak
    Og at det finnes materiale som er utlånt til låneren
    Og at materialet har en eieravdeling

  Scenario: Noen forsøker å levere tilbake lånt materiale på utlånsautomat
    Gitt at materialet har riktig antall RFID-brikker
    Og at det ikke finnes en reservasjon på materialet
    Og at materialets henteavdeling er lik den avdelingen der materialet blir levert
    Når innlevering blir valgt på automaten
    Og låneren legger materialet på automaten
    Så får låneren beskjed om at materialet er registrert levert
    Og det gis beskjed om at materialet skal settes på oppstillingshylle
    Og systemet viser at låneren ikke låner materialet
    Og systemet viser at alarm er aktivert

  Scenario: Noen forsøker å levere tilbake lånt materiale som tilhører annen avdeling på utlånsautomat
    Gitt at materialet har riktig antall RFID-brikker
    Og at det ikke finnes en reservasjon på materialet
    Og at materialets henteavdeling ikke er lik den avdelingen der materialet blir levert
    Når innlevering blir valgt på automaten
    Og låneren legger materialet på automaten
    Så registrerer automaten at materialet tilhører annen avdeling
    Og det gis beskjed om at materialet skal legges i innleveringsboks

  @wip @issue-11 @unstable
  Scenario: Noen forsøker å levere tilbake lånt materiale som er reservert på utlånsautomat
    Gitt at materialet har riktig antall RFID-brikker
    Og at det finnes en reservasjon på materialet
    Når innlevering blir valgt på automaten
    Og låneren legger materialet på automaten
    Så registrerer automaten at materialet er reservert på annen avdeling
    Og det gis beskjed om at materialet skal legges i innleveringsboks
