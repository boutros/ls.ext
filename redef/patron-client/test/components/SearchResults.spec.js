import expect from 'expect'
import React from 'react'
import TestUtils from 'react-addons-test-utils'
import SearchResults from '../../src/frontend/components/SearchResults'
import ReactDOM from 'react-dom'

function setup (propOverrides) {
  const props = Object.assign({
    locationQuery: {},
    searchError: false,
    totalHits: 0,
    searchResults: [],
    searchActions: { search: expect.createSpy() }
  }, propOverrides)

  const output = TestUtils.renderIntoDocument(
    <SearchResults {...props} />
  );

  return {
    props: props,
    output: output,
    node: ReactDOM.findDOMNode(output)
  }
}

describe('components', () => {
  describe('SearchResults', () => {
    it('should render no search', () => {
      const { node, props } = setup()
      expect(node.querySelectorAll("[data-automation-id='no-search']").length).toBe(1)
    })

    it('should search on mount when query is provided', () => {
      const { node, props } = setup({ locationQuery: { query: 'test_query' } })
      expect(props.searchActions.search).toHaveBeenCalled()
    })

    it('should render search term', () => {
      const { node, props } = setup({ locationQuery: { query: 'test_query' } })
      expect(node.querySelector("[data-automation-id='current-search-term']").textContent).toEqual(props.locationQuery.query)
    })

    it('should render search error', () => {
      const { node, props } = setup({ searchError: true })
      expect(node.getAttribute('data-automation-id')).toBe('search-error')
    })

    it('should render the correct number of results', () => {
      const { node, props } = setup({
        locationQuery: { query: 'test_query' },
        searchResults: [ {
          originalTitle: 'test_originalTitle1',
          mainTitle: 'test_mainTitle1',
          creators: [ {
            name: 'test_creator_name1',
            relativeUri: 'test_creator_relativeUri1'
          } ],
          relativeUri: 'test_relativeUri1'
        }, {
          originalTitle: 'test_originalTitle2',
          mainTitle: 'test_mainTitle2',
          creators: [ {
            name: 'test_creator_name2',
            relativeUri: 'test_creator_relativeUri2'
          } ],
          relativeUri: 'test_relativeUri2'
        }, {
          originalTitle: 'test_originalTitle3',
          mainTitle: 'test_mainTitle3',
          creators: [ {
            name: 'test_creator_name3',
            relativeUri: 'test_creator_relativeUri3'
          } ],
          relativeUri: 'test_relativeUri3'
        } ],
        totalHits: 3
      })

      expect(node.querySelector("[data-automation-id='search-result-entries']").childNodes.length).toBe(3)
    })

  })
})
