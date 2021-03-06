# encoding: UTF-8
require 'uri'
require 'net/http'

Given(/^at det finnes et verk$/) do
  steps %Q{
   Gitt at jeg er i katalogiseringsgrensesnittet
   Så leverer systemet en ny ID for det nye verket
   Og jeg kan legge til tittel for det nye verket
   Når jeg legger til et årstall for førsteutgave av nye verket
   Så grensesnittet viser at endringene er lagret
  }
end

Given(/^at det finnes et verk med tre ledd i tittelen$/) do
  steps %Q{
   Gitt at jeg er i katalogiseringsgrensesnittet
   Så leverer systemet en ny ID for det nye verket
   Og jeg kan legge til tittel med tre ledd for det nye verket
   Når jeg legger til et årstall for førsteutgave av nye verket
   Så grensesnittet viser at endringene er lagret
  }
end

Given(/^et verk med en utgivelse$/) do
  step "at det finnes et verk"
  step "at det finnes en utgivelse"
end

Gitt(/^et verk med flere utgivelser og eksemplarer$/) do
  step "at det finnes et verk"

  # we need Koha to be set up
  steps %Q{
    Gitt at jeg er logget inn som adminbruker
    Gitt at det finnes en avdeling
    Når jeg legger til en materialtype
  }

  # Add 3 publications of the work, with 2 exemplars of each
  3.times do
    step "at det finnes en utgivelse"
    step "får utgivelsen tildelt en post-ID i Koha" # needed to store :publication_recordid in context
    2.times do
      # create new item
      page = @site.BiblioDetail.visit(@context[:publication_recordid])
      page.add_item_with_random_barcode_and_itemtype(@context[:itemtypes][0].desc)
    end
    record_id = @context[:publication_recordid] # need to store it in a variable so the cleanup can close over it
    @cleanup.push("delete items of biblio ##{record_id}" =>
                      lambda do
                        page = @site.BiblioDetail.visit(record_id)
                        page.delete_all_items
                        # TODO: deletion of biblio will be handled by services?
                      end
    )
  end
end


Given(/^at det finnes et verk med biblio-kobling$/) do
  step "at det finnes et verk"
  @site.RegWork.add_prop("http://#{ENV['HOST']}:8005/ontology#biblio", @context[:biblio])
  step "grensesnittet viser at endringene er lagret"
end

Given(/^at det finnes et eksemplar av en bok registrert i Koha/) do
  steps %Q{
    Gitt at jeg er logget inn som adminbruker
    Gitt at det finnes en avdeling
    Når jeg legger til en materialtype
  }
  book = SVC::Biblio.new(@browser, @context, @active).add
  @active[:book] = book
  @context[:biblio] = book.biblionumber
  @context[:publication_maintitle] = book.title
  @context[:publication_recordid] = book.biblionumber

  @cleanup.push("bok #{book.biblionumber}" =>
                    lambda do
                      SVC::Biblio.new(@browser).delete(book)
                    end
  )
end

Given(/^at jeg er i katalogiseringsgrensesnittet$/) do
  @site.RegWork.visit
end

Given(/^at det er en feil i systemet for katalogisering$/) do
  `docker stop redef_services_container`
  @cleanup.push("restarting redef_services_container" =>
                    lambda do
                      `sudo docker start redef_services_container`
                      sleep 15 # give container time to get up running properly for next tests
                    end
  )
end

Given(/^at systemet har returnert en ny ID for det nye verket$/) do
  step "leverer systemet en ny ID for det nye verket"
end

Given(/^at det er en feil i systemet som behandler katalogisering$/) do
  pending # express the regexp above with the code you wish you had
end

Given(/^at verket har en tittel$/) do
  step "jeg kan legge til tittel for det nye verket"
  step "grensesnittet viser at endringene er lagret"
end

Given(/^at det finnes et verk med feil årstall$/) do
  step "at jeg er i katalogiseringsgrensesnittet"
  step "at systemet har returnert en ny ID for det nye verket"
  step "jeg legger til et årstall for førsteutgave av nye verket"
end

Given(/^at jeg ser på et lagret verk$/) do
  step "at jeg er i katalogiseringsgrensesnittet"
  step "at systemet har returnert en ny ID for det nye verket"
  step "jeg kan legge til tittel for det nye verket"
  step "grensesnittet viser at endringene er lagret"
end

Given(/^at jeg ser på et lagret verk med biblio\-koblinger$/) do
  step "at det finnes et eksemplar av en bok registrert i Koha"
  step "at jeg er i katalogiseringsgrensesnittet"
  step "at jeg ser på et lagret verk"
  @site.RegWork.add_prop("http://#{ENV['HOST']}:8005/ontology#biblio", @context[:biblio])
end

Given(/^at det finnes en utgivelse$/) do
  step "jeg registrerer inn opplysninger om utgivelsen"
  step "jeg knytter utgivelsen til verket"
end

Given(/^at det finnes en utgivelse uten verk$/) do
  step "jeg registrerer inn opplysninger om utgivelsen"
end

Given(/^at det finnes et verk og en utgivelse$/) do
  steps %Q{
    Gitt at det finnes et verk
    Og at det finnes en utgivelse
    Og får utgivelsen tildelt en post-ID i Koha
  }
end

Given(/^at det finnes et verk med person og en utgivelse$/) do
  steps %Q{
    Gitt at jeg har lagt til en person
    Og at jeg er i katalogiseringsgrensesnittet
    Og at systemet har returnert en ny ID for det nye verket
    Og jeg legger til forfatter av det nye verket
    Og jeg registrerer inn opplysninger om utgivelsen
    Og jeg knytter utgivelsen til verket
  }
end

Given(/^at det finnes et verk med person$/) do
  steps %Q{
    Gitt at jeg har lagt til en person
    Og at jeg er i katalogiseringsgrensesnittet
    Og at systemet har returnert en ny ID for det nye verket
    Og jeg legger til forfatter av det nye verket
  }
end

When(/^jeg ser på utgivelsen i katalogiseringsgrensesnittet$/) do
  true
end

When(/^jeg klikker på lenken til en biblio\-kobling$/) do
  @browser.goto(@browser.a(:class => "biblio_record_link").href)
end

When(/^jeg klikker på lenken til verks\-siden$/) do
  @browser.goto(@site.RegWork.get_link)
end

When(/^jeg følger lenken til posten i Koha$/) do
  link = @browser.a(:data_automation_id => "biblio_record_link").href
  steps %Q{
    Gitt at jeg er logget inn som adminbruker
    Gitt at det finnes en avdeling
    Når jeg legger til en materialtype
  }
  @browser.goto(@browser.url[0..@browser.url.index("/cgi-bin")] + link[link.index("/cgi-bin")+1..-1])
end

Then(/^kommer jeg til Koha's presentasjon av biblio$/) do
  step "verkets tittel vises på verks-siden"
end

When(/^jeg åpner utgivelsen i gammelt katalogiseringsgrensesnitt$/) do
  unless (@context[:publication_identifier])
    fail 'No publication identifier added to context'
  end
  step 'jeg åpner utgivelsen for redigering'
end

When(/^jeg åpner verket i gammelt katalogiseringsgrensesnitt$/) do
  unless (@context[:work_identifier])
    fail 'No work identifier added to context'
  end
  step 'jeg åpner verket for redigering'
end

When(/^jeg åpner verket for redigering$/) do
  @site.RegWork.open(@context[:work_identifier], "work")
end

When(/^jeg åpner utgivelsen for redigering$/) do
  @site.RegPublication.open(@context[:publication_identifier])
end

When(/^jeg åpner personen for redigering$/) do
  @site.RegPerson.open(@context[:person_identifier])
end


When(/^når jeg endrer årstall for førsteutgave til verket$/) do
  step "jeg legger til et årstall for førsteutgave av nye verket"
  step "grensesnittet viser at endringene er lagret"
end

When(/^jeg legger til en inn alternativ tittel på det nye verket$/) do
  predicate = "http://#{ENV['HOST']}:8005/ontology#mainTitle"
  @context[:alt_title] = generateRandomString
  @browser.div(:class => predicate).button.click
  @site.RegWork.add_prop(predicate, @context[:alt_title], 1)
end

When(/^jeg legger til tittel for det nye verket$/) do
  step "jeg kan legge til tittel for det nye verket"
end

When(/^jeg vil legge til et nytt verk$/) do
  true
end

When(/^jeg forsøker å registrere ett nytt verk$/) do
  @context[:work_maintitle] = generateRandomString
  @site.RegWork.add_prop_skip_wait("http://#{ENV['HOST']}:8005/ontology#mainTitle", @context[:work_maintitle])
end

When(/^jeg velger språk for tittelen$/) do
  predicate = "http://#{ENV['HOST']}:8005/ontology#mainTitle"
  @context[:title_lang] = "no"
  @browser.div(:class => predicate).select.select_value(@context[:title_lang])
end

When(/^jeg legger til et årstall for førsteutgave av nye verket$/) do
  @context[:work_publicationyear] = (rand(1015)+1000).to_s
  @site.RegWork.add_prop("http://#{ENV['HOST']}:8005/ontology#publicationYear", @context[:work_publicationyear])
end

When(/^jeg sletter eksisterende forfatter på verket$/) do
  tries = 3
  begin
    deletables = @browser.inputs(:class => 'deletable').size
    @browser.inputs(:data_automation_id => "http://#{ENV['HOST']}:8005/ontology#creator"+"_0").first.click
    Watir::Wait.until(BROWSER_WAIT_TIMEOUT) { @browser.inputs(:class => 'deletable').size == deletables - 1 } #Allow some time for the UI to update after clicking the red X
  rescue Watir::Wait::TimeoutError
    STDERR.puts "TIMEOUT: retrying .... #{(tries -= 1)}"
    if (tries == 0)
      fail
    else
      retry
    end
  end
end

When(/^jeg legger til forfatter av det nye verket$/) do
  step "jeg søker på navn til opphavsperson for det nye verket"
  step "velger person fra en treffliste"
end

When(/^jeg søker på navn til opphavsperson for det nye verket$/) do
  tries = 3
  begin
    @site.RegWork.search_resource("http://#{ENV['HOST']}:8005/ontology#creator", @context[:person_name])
    Watir::Wait.until(BROWSER_WAIT_TIMEOUT) { @browser.div(:data_automation_id => @context[:person_identifier]) }
  rescue Watir::Wait::TimeoutError
    STDERR.puts "TIMEOUT: retrying .... #{(tries -= 1)}"
    if (tries == 0)
      fail
    else
      retry
    end
  end
end

When(/^velger person fra en treffliste$/) do
  @site.RegWork.select_resource(@context[:person_identifier].to_s)
  @context[:work_creator] = @context[:person_name]
end

When(/^jeg legger inn "(.*?)" i feltet for førsteutgave av verket$/) do |arg1|
  @context[:work_publicationyear] = arg1
  @site.RegWork.add_prop_skip_wait("http://#{ENV['HOST']}:8005/ontology#publicationYear", @context[:work_publicationyear])
end

When(/^jeg legger til et eksemplar av utgivelsen$/) do
  pending # express the regexp above with the code you wish you had
end

When(/^jeg vil katalogisere en utgivelse$/) do
  @site.RegPublication.visit
end

When(/^jeg oppretter et eksemplar av utgivelsen$/) do
  @context[:item_barcode] = '0301%010d' % rand(10 ** 10)
  @browser.button(:text => "New").click
  @browser.link(:id => "newitem").click
  @browser.select_list(:id => /^tag_952_subfield_y_[0-9]+$/).select(@context[:itemtypes][0].desc)
  @browser.text_field(:id => /^tag_952_subfield_p_[0-9]+$/).set(@context[:item_barcode])
  @browser.text_field(:id => /^tag_952_subfield_o_[0-9]+$/).set('%d%d%d %s%s%s' % [rand(10), rand(10), rand(10), ('A'..'Z').to_a.shuffle[0], ('a'..'z').to_a.shuffle[0], ('a'..'z').to_a.shuffle[0]])
  @browser.button(:text => "Add item").click
  record_id = @context[:publication_recordid]

  @cleanup.push("delete items of biblio ##{record_id}" =>
                    lambda do
                      @browser.goto intranet(:biblio_detail)+record_id

                      # delete all book items
                      @browser.execute_script("window.confirm = function(msg){return true;}")
                      @browser.button(:text => "Edit").click
                      @browser.a(:id => "deleteallitems").click

                      # TODO: deletion of biblio will be handled by services?
                    end
  )
end

Then(/^får utgivelsen tildelt en post\-ID i Koha$/) do
  @context[:publication_recordid] = @site.RegPublication.get_record_id
  @context[:publication_recordid].should_not be_empty
end

Then(/^det vises en lenke til posten i Koha i katalogiseringsgrensesnittet$/) do
  link = @browser.a(:data_automation_id => "biblio_record_link")
  link.href.end_with?("biblionumber=#{@context[:publication_recordid]}").should be true
end

Then(/^viser systemet at "(.*?)" ikke er ett gyldig årstall$/) do |arg1|
  @browser.element(:text => "ugyldig input").present?
end

Then(/^viser systemet at årstall for førsteutgave av verket har blitt registrert$/) do
  step "grensesnittet viser at endringene er lagret"
end

Then(/^viser systemet at språket til tittelen blitt registrert$/) do
  step "grensesnittet viser at endringene er lagret"
end

Then(/^leverer systemet en ny ID for det nye verket$/) do
  @context[:work_identifier] = @site.RegWork.get_id()
  @context[:work_identifier].should_not be_empty
end

Then(/^jeg kan legge til tittel for det nye verket$/) do
  @context[:work_maintitle] = generateRandomString
  @site.RegWork.add_prop("http://#{ENV['HOST']}:8005/ontology#mainTitle", @context[:work_maintitle])
end

Then(/^jeg kan legge til språk for det nye verket$/) do
  @context[:work_lang] = "http://lexvo.org/id/iso639-3/nob"
  @site.RegWork.select_prop("http://#{ENV['HOST']}:8005/ontology#language", "Norsk (bokmål)")
end

Then(/^jeg kan legge til tittel for den nye utgivelsen$/) do
  @context[:publication_maintitle] = generateRandomString
  @site.RegPublication.add_prop("http://#{ENV['HOST']}:8005/ontology#mainTitle", @context[:publication_maintitle])
end

Then(/^jeg kan legge til tittel med tre ledd for det nye verket$/) do
  @context[:work_maintitle] = [generateRandomString, generateRandomString, generateRandomString].join(' ')
  @site.RegWork.add_prop("http://#{ENV['HOST']}:8005/ontology#mainTitle", @context[:work_maintitle])
end

Then(/^grensesnittet viser at endringene er lagret$/) do
  Watir::Wait.until(BROWSER_WAIT_TIMEOUT) { @browser.div(:id => /save-stat/).text === "alle endringer er lagret" }
end

Then(/^får jeg beskjed om at noe er feil$/) do
  @site.RegWork.errors.should include("Noe gikk galt!")
end

Then(/^viser systemet at tittel på verket har blitt registrert$/) do
  step "grensesnittet viser at endringene er lagret"
end

Then(/^viser systemet at alternativ tittel på verket har blitt registrert$/) do
  step "grensesnittet viser at endringene er lagret"
end

When(/^jeg registrerer inn opplysninger om utgivelsen$/) do
  page = nil
  if @browser.driver.browser == :phantomjs
    # Because phantomjs caches redirects, we need to perform the get request ourself,
    # store the URI and then open it:
    res = Net::HTTP.get_response(URI(catalinker("publication")))
    uri = res['location'][res['location'].index("resource=")+9..-1]
    page = @site.RegPublication.open(uri)
  else
    page = @site.RegPublication.visit
  end

  @context[:publication_identifier] = @site.RegPublication.get_link
  @context[:publication_format] = ['Bok', 'CD', 'DVD', 'CD-ROM', 'DVD-ROM'].sample
  @context[:publication_language] = ['Engelsk', 'Norsk (bokmål)', 'Finsk', 'Baskisk', 'Grønlandsk'].sample
  @context[:publication_maintitle] = generateRandomString
  step "får utgivelsen tildelt en post-ID i Koha"

  page.select_prop("http://#{ENV['HOST']}:8005/ontology#format", @context[:publication_format])
  page.select_prop("http://#{ENV['HOST']}:8005/ontology#language", @context[:publication_language])
  page.add_prop("http://#{ENV['HOST']}:8005/ontology#mainTitle", @context[:publication_maintitle])
end


When(/^jeg knytter utgivelsen til verket$/) do
  page = @site.RegPublication
  page.add_prop("http://#{ENV['HOST']}:8005/ontology#publicationOf", @context[:work_identifier])
end

Given(/^et verk med en utgivelse og et eksemplar$/) do
  steps %Q{
    Gitt at det finnes et verk og en utgivelse
    Når jeg ser på utgivelsen i katalogiseringsgrensesnittet
    Og jeg følger lenken til posten i Koha
    Og jeg oppretter et eksemplar av utgivelsen
  }
end

Given(/^at det finnes en personressurs$/) do
  step "at jeg har lagt til en person"
  step "grensesnittet viser at personen er lagret"
end

Given(/^at jeg er i personregistergrensesnittet$/) do
  @site.RegPerson.visit
end

When(/^jeg vil legge til en person$/) do
  true
end

When(/^jeg vil lage et nytt utgivelsessted$/) do
  @site.RegPlaceOfPublication.visit
end

When(/^leverer systemet en ny ID for den nye personen$/) do
  @context[:person_identifier] = @site.RegPerson.get_id()
  @context[:person_identifier].should_not be_empty
end

When(/^leverer systemet en ny ID for det nye utgivelsesstedet$/) do
  @context[:placeofpublication_identifier] = @site.RegPlaceOfPublication.get_id()
  @context[:placeofpublication_identifier].should_not be_empty
end

When(/^jeg kan legge inn navn fødselsår og dødsår for personen$/) do
  @context[:person_name] = generateRandomString
  @site.RegPerson.add_prop("http://#{ENV['HOST']}:8005/ontology#name", @context[:person_name])

  @context[:person_birthyear] = (1000 + rand(1015)).to_s
  @site.RegPerson.add_prop("http://#{ENV['HOST']}:8005/ontology#birthYear", @context[:person_birthyear])

  @context[:person_deathyear] = (1000 + rand(1015)).to_s
  @site.RegPerson.add_prop("http://#{ENV['HOST']}:8005/ontology#deathYear", @context[:person_deathyear])
end

When(/^jeg kan legge inn stedsnavn og land$/) do
  @context[:placeofpublication_place] = generateRandomString
  @site.RegPlaceOfPublication.add_prop("http://#{ENV['HOST']}:8005/ontology#place", @context[:placeofpublication_place])
  @context[:placeofpublication_country] = generateRandomString
  @site.RegPlaceOfPublication.add_prop("http://#{ENV['HOST']}:8005/ontology#country", @context[:placeofpublication_country])
end

When(/^jeg kan legge inn tittel og nasjonalitet for personen$/) do
  @context[:person_title] = generateRandomString
  @site.RegPerson.add_prop("http://#{ENV['HOST']}:8005/ontology#personTitle", @context[:person_title])

  @context[:person_nationality] = ['Norsk', 'Engelsk', 'Færøyisk'].sample
  @site.RegPerson.select_prop("http://#{ENV['HOST']}:8005/ontology#nationality", @context[:person_nationality])
end


When(/^grensesnittet viser at personen er lagret$/) do
  Watir::Wait.until(BROWSER_WAIT_TIMEOUT) { @browser.div(:id => /save-stat/).text === "alle endringer er lagret" }
end

When(/^jeg klikker på linken ved urien kommer jeg til personsiden$/) do
  @browser.goto(@site.RegPerson.get_link)
end

When(/^personens navn vises på personsiden$/) do
  Watir::Wait.until(BROWSER_WAIT_TIMEOUT) { @site.PatronClientPersonPage.getPersonName.include? @context[:person_name] }
end

When(/^at jeg har lagt til en person$/) do
  steps %Q{
    Gitt at jeg er i personregistergrensesnittet
    Så leverer systemet en ny ID for den nye personen
    Og jeg kan legge inn navn fødselsår og dødsår for personen
    Og jeg kan legge inn tittel og nasjonalitet for personen
  }
end

When(/^viser systemet at opphavsperson til verket har blitt registrert$/) do
  step "grensesnittet viser at endringene er lagret"
end

When(/^når jeg endrer tittelen på utgivelsen$/) do
  step "jeg kan legge til tittel for den nye utgivelsen"
  step "grensesnittet viser at endringene er lagret"
end

When(/^når jeg endrer tittelen på verket$/) do
  step "jeg kan legge til tittel for det nye verket"
  step "grensesnittet viser at endringene er lagret"
end

When(/^når jeg endrer navnet på personen$/) do
  @context[:person_name] = generateRandomString
  @context[:work_creator] = @context[:person_name]
  @site.RegPerson.add_prop("http://#{ENV['HOST']}:8005/ontology#name", @context[:person_name])
  step "grensesnittet viser at endringene er lagret"
end

When(/^jeg endrer forfatteren på verket$/) do
  step "jeg sletter eksisterende forfatter på verket"
  step "jeg legger til forfatter av det nye verket"
  step "grensesnittet viser at endringene er lagret"
end

When(/^jeg venter litt$/) do
  sleep 5
  @browser.execute_script("console.log('waiting...')")
end

When(/^viser trefflisten at personen har et verk fra før$/) do
  Watir::Wait.until(BROWSER_WAIT_TIMEOUT) { @browser.span(:class => "search-result-name", :text => @context[:person_name]).should exist }
end

When(/^trefflisten viser at personen har riktig nasjonalitet$/) do
  Watir::Wait.until(BROWSER_WAIT_TIMEOUT) { @browser.span(:class => "nationality", :text => @context[:person_nationality]).should exist }
end

When(/^trefflisten viser at personen har riktig levetid$/) do
  Watir::Wait.until(BROWSER_WAIT_TIMEOUT) { @browser.span(:class => "birthYear", :text => @context[:person_birthyear]).should exist }
  Watir::Wait.until(BROWSER_WAIT_TIMEOUT) { @browser.span(:class => "deathYear", :text => @context[:person_deathyear]).should exist }
end

When(/^jeg verifiserer opplysningene om utgivelsen$/) do
  # TODO: Unify get_prop and get_select_prop in the page objects to avoid having to specify it.
  data = Hash.new
  data['mainTitle'] = :get_prop
  data['subtitle'] = :get_prop
  data['publicationYear'] = :get_prop
  data['format'] = :get_select_prop
  data['language'] = :get_select_prop
  data['partTitle'] = :get_prop
  data['partNumber'] = :get_prop
  data['edition'] = :get_prop
  data['numberOfPages'] = :get_prop
  data['isbn'] = :get_prop
  data['illustrativeMatter'] = :get_select_prop
  data['adaptationOfPublicationForParticularUserGroups'] = :get_select_prop
  data['binding'] = :get_select_prop
  data['writingSystem'] = :get_select_prop

  batch_verify_props @site.RegPublication, 'Publication', data
end

def batch_verify_props(page_object, domain, data, locate_by = 'locate_by_fragment'.to_sym)
  data.each do |id, method|
    if locate_by == :locate_by_fragment
      symbol = "#{domain.downcase}_#{id.downcase}".to_sym # e.g. :publication_format
      inputLocator = "http://#{ENV['HOST']}:8005/ontology##{id}"
    else
      symbol = id
      inputLocator = id
    end

    begin
      expected_value = @context[symbol] || @context["#{id.downcase}_identifier".to_sym]
      if expected_value.kind_of?(Array)
        actual_values = Array.new
        expected_value.length.times do |index|
          actual_values.push page_object.method(method).call(inputLocator, index)
        end
        actual_values.should=~expected_value
      else
        page_object.method(method).call(inputLocator).should eq expected_value
      end
    rescue
      fail "Failed getting field for #{id}"
    end
    fail "More than one field for #{id}" if page_object.get_prop_count(inputLocator && !expected_value.kind_of?(Array)) > 1
  end
end

When(/^jeg verifiserer opplysningene om verket$/) do
  data = Hash.new
  data['mainTitle'] = :get_prop
  data['subtitle'] = :get_prop
  data['publicationYear'] = :get_prop
  data['language'] = :get_select_prop

  batch_verify_props @site.RegWork, 'Work', data
end

def verify_fragment(fragment, prop_get_method)
  resource_type = @browser.span(:id, "resource-type").attribute_value("data-resource-type")
  data = Hash.new
  data[fragment] = prop_get_method
  batch_verify_props @site.RegWork, resource_type, data, :locate_by_label
end

def verify_by_label(label, prop_get_method)
  resource_type = @browser.span(:id, "resource-type").attribute_value("data-resource-type")
  data = Hash.new
  data[label] = prop_get_method
  batch_verify_props @site.RegWork, resource_type, data, :locate_by_label
end

When(/^verifiserer jeg innskrevet verdi for "([^"]*)"$/) do |fragment|
  verify_fragment(fragment, :get_prop_by_label)
end

When(/^verifiserer jeg valgt verdi for "([^"]*)"$/) do |label|
  verify_fragment(label, :get_select_prop_by_label)
end

When(/^verifiserer jeg valgte verdier for "([^"]*)"$/) do |label|
  verify_fragment(label, :get_select_prop_by_label)
end

When(/^at utgivelsen er tilkoplet riktig utgivelsessted$/) do
  data = Hash.new
  data["placeOfPublication"] = :get_prop
  batch_verify_props @site.RegPublication, 'Publication', data, :locate_by_fragment
end