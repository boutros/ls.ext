# encoding: utf-8

require_relative 'page.rb'

class IntraPage < Page

  def search_catalog(query)
    @browser.a(:href => "#catalog_search").click
    form = @browser.form(:id => "cat-search-block")
    form.text_field(:id => "search-form").set query
    form.submit
    CatalogueDetail.new(@browser)
  end

  def select_branch(branch_name=nil)
    SelectBranch.new(@browser).go.select_branch(branch_name)
  end

  def checkin(barcode)
    @browser.a(:href => "#checkin_search").click
    @browser.text_field(:id => "ret_barcode").set barcode
    @browser.form(:action => "/cgi-bin/koha/circ/returns.pl").submit
    # todo -- where do we end up?
  end

  def search_patrons(patron_query)
    @browser.a(:href => "#patron_search").click
    @browser.text_field(:id => "searchmember").set patron_query
    @browser.form(:action => "/cgi-bin/koha/members/member.pl").submit
    @browser.div(:class => 'patroninfo').wait_until_present
    PatronDetails.new(@browser)
  end

  def logged_in (userid)
    @browser.span(:class => 'loggedinusername').exists? &&
        @browser.span(:class => 'loggedinusername').text.strip == userid
  end
end