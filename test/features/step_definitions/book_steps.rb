# encoding: UTF-8

Given(/^at det finnes en materialtype for "(.*?)" med kode "(.*?)"$/) do |name, code|
  step "jeg legger til en materialtype \"#{name}\" med kode \"#{code}\""
end

Given(/^at det finnes en bok$/) do
  step "jeg legger til en materialtype \"Bok\" med kode \"L\""
  step "at det finnes en avdeling"
  step "jeg legger inn \"Fargelegg byen!\" som ny bok"
end

Given(/^at "(.*?)" er ei bok som finnes i biblioteket$/) do |book|
  step "jeg legger til en materialtype \"Bok\" med kode \"L\""
  step "jeg legger inn \"#{book}\" som ny bok"
end

When(/^jeg legger inn "(.*?)" som ny bok$/) do |book|
  @http = Net::HTTP.new(host, 8081)
  res = @http.get("/cgi-bin/koha/svc/authentication?userid=#{SETTINGS['koha']['adminuser']}&password=#{SETTINGS['koha']['adminpass']}")
  res.body.should_not include("failed")
  @context[:svc_cookie] = res.response['set-cookie']
  data = File.read("features/upload-files/#{book}.normarc")
  headers = {
    'Cookie' => @context[:svc_cookie],
    'Content-Type' => 'text/xml'
  }
  res, data = @http.post("/cgi-bin/koha/svc/new_bib?items=1", data, headers)
  res.body.should include("<status>ok</status>")
  @context[:book_id] = res.body.match(/<biblionumber>(\d+)<\/biblionumber>/)[1]
  @context[:barcode] = res.body.match(/<subfield code="p">(\d+)<\/subfield>/)[1]
  # force rebuild and restart zebra bibliographic index
  `ssh -i ~/.ssh/insecure_private_key vagrant@192.168.50.10 \
    -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no \
    'sudo koha-rebuild-zebra name -b -v \
    && sudo koha-stop-zebra name \
    && sudo koha-start-zebra name' > /dev/null 2>&1`
end

When(/^jeg legger til en materialtype "(.*?)" med kode "(.*?)"$/) do |name, code|
  @browser.goto intranet(:item_types)
  @browser.a(:id => "newitemtype").click
  form = @browser.form(:id => "itemtypeentry")
  form.text_field(:id => "itemtype").set code
  form.text_field(:id => "description").set name
  @context[:item_type_name] = name
  @context[:item_type_code] = code
  form.submit
end

Then(/^kan jeg se materialtypen i listen over materialtyper$/) do
  @browser.goto intranet(:item_types)
  table = @browser.table(:id => "table_item_type")
  table.should be_present
  table.text.should include(@context[:item_type_name])
  table.text.should include(@context[:item_type_code])
end

Then(/^viser systemet at "(.*?)" er en bok som kan lånes ut$/) do |book|
  @browser.goto "http://#{host}:8081/cgi-bin/koha/catalogue/detail.pl?biblionumber=#{@context[:book_id]}"
  @browser.div(:id => "catalogue_detail_biblio").text.should include(book)
  holdings = @browser.div(:id => "holdings")
  holdings.text.should include("Available")
  barcode = holdings.tbody.tr.td(:index => 7).text
end

Then(/^kan jeg søke opp boka$/) do
  @browser.goto intranet(:home)
  @browser.a(:text => "Search the catalog").click
  form = @browser.form(:id => "cat-search-block")
  form.text_field(:id => "search-form").set("Fargelegg byen") # if we include the exclamation mark, we get no results
  form.submit
  @browser.text.should include("Fargelegg byen!")
  @browser.text.should include("Available")
end
