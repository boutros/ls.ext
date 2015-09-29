# encoding: UTF-8

When(/^jeg søker på verket i lånergrensesnittet$/) do
  page = @site.SearchPatronClient
  page.visit
  page.search_with_text('Sult')
end

Then(/^vil jeg finne verket i trefflista$/) do
  resultList = @site.SearchPatronClient.get_search_result_list
  resultList.text.include?('Sult').should == true
end