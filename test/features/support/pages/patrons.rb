# encoding: utf-8

require_relative 'intra_page.rb'

class Patrons < IntraPage
  def go
    @browser.goto intranet(:patrons)
    self
  end

  def create(categorydesc, firstname, surname, userid, passwd)
    @browser.button(:text => "New patron").click
    @browser.div(:class => "btn-group").ul(:class => "dropdown-menu").a(:text => categorydesc).click
    form = @browser.form(:name => "form")
    form.text_field(:id => "firstname").set firstname
    form.text_field(:id => "surname").set surname
    form.text_field(:id => "userid").set userid
    form.text_field(:id => "password").set passwd
    form.text_field(:id => "password2").set passwd
    form.button(:name => "save").click
  end

  def search(query)
    form = @browser.form(:id => "searchform")
    form.text_field(:id => "searchmember_filter").set query
    form.submit
    @browser.div(:class => 'patroninfo').wait_until_present
    PatronDetails.new(@browser)
  end

  def delete(name, surname)
    search("#{name} #{surname}").delete
  end
end