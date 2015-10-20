# encoding: UTF-8

require_relative '../page_root.rb'

class PatronClientWorkPage < PageRoot
    def visit(workId)
      @browser.goto patron_client(:work) + "/" + workId
      self
    end

    def getTitle
      return @browser.h2(:data_automation_id => /work_title/).text
    end

    def getAuthor
      return @browser.span(:data_automation_id => /work_author/).text
    end

    def getDate
      return @browser.span(:data_automation_id => /work_date/).text
    end

    def existsExemplar
      if @browser.td(:data_automation_id => /item_location/).text
        location = true
      else
        location = false
      end
      return location
    end

    def getPublicationsTableRows
      return @browser.div(:id=> "publications").table.tbody.rows
    end

    def getItemsTableRows
      return @browser.div(:id=> "items").table.tbody.rows
    end
end