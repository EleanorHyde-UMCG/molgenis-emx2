import { request, gql } from "graphql-request";

export default {
  fetchVariables: async ({ commit, getters }) => {
    const query = gql`
      query Variables($search: String, $filter: VariablesFilter) {
        Variables(limit: 100, search: $search, filter: $filter) {
          name
          release {
            resource {
              acronym
            }
            version
          }
          label
          repeats {
            name
          }
        }
        Variables_agg(filter: $filter) {
          count
        }
      }
    `;
    let variables = {
      filter: {
        release: {
          equals: [
            {
              resource: {
                acronym: "LifeCycle",
              },
              version: "1.0.0",
            },
          ],
        },
      },
    };

    if (getters.selectedKeywords.length) {
      variables.filter.keywords = {
        equals: getters.selectedKeywords.map((sk) => ({ name: sk })),
      };
    }

    const resp = await request("graphql", query, variables).catch((e) =>
      console.error(e)
    );
    commit("setVariables", resp.Variables);
    commit("setVariableCount", resp.Variables_agg.count);
  },

  fetchVariableDetails: async ({ commit, getters }, variableName) => {
    if (getters.variableDetails[variableName]) {
      // cache hit
      return getters.variableDetails[variableName];
    }
    // else fetch
    const query = gql`
      query Variables($filter: VariablesFilter) {
        Variables(limit: 1, filter: $filter) {
          name
          label
          format {
            name
          }
          unit {
            name
          }
          description
          repeats {
            name
          }
        }
      }
    `;
    const variables = {
      filter: {
        name: {
          like: [`${variableName}`],
        },
        release: {
          equals: [
            {
              resource: {
                acronym: "LifeCycle",
              },
              version: "1.0.0",
            },
          ],
        },
      },
    };

    const resp = await request("graphql", query, variables).catch((e) =>
      console.error(e)
    );
    commit("setVariableDetails", {
      variableName,
      variableDetails: resp.Variables[0],
    });
  },

  fetchKeywords: async ({ commit }) => {
    const keywordQuery = gql`
      query Keywords {
        Keywords {
          name
          definition
          order
          parent {
            name
          }
        }
      }
    `;
    const keyWordResp = await request("graphql", keywordQuery).catch((e) =>
      console.error(e)
    );
    commit("setKeywords", keyWordResp.Keywords);
  },

  fetchCohorts: async ({ commit }) => {
    const query = gql`
      query Databanks {
        Databanks {
          acronym
          name
          type {
            name
          }
        }
      }
    `;
    //{filter: {type: {equals: [{name: "cohort"}, {name: "harmonisation"}]}}}
    const resp = await request("graphql", query).catch((e) => console.error(e));
    commit("setCohorts", resp.Databanks);
  },

  fetchMappings: async ({ commit, getters }) => {
    const query = gql`
      query VariableMappings($filter: VariableMappingsFilter) {
        VariableMappings(limit: 100, filter: $filter) {
          fromTable {
            release {
              resource {
                acronym
              }
              version
            }
            name
          }
          toVariable {
            table {
              release {
                resource {
                  acronym
                }
                version
              }
              name
            }
            name
          }
          match {
            name
          }
        }
      }
    `;

    const variables = getters.variables.map((v) => {
      return {
        release: {
          resource: {
            acronym: "LifeCycle",
          },
          version: "1.0.0",
        },
        name: v.name,
      };
    });

    const filter = variables.length
      ? {
          filter: { toVariable: { equals: variables } },
        }
      : {};

    const resp = await request("graphql", query, filter).catch((e) =>
      console.error(e)
    );
    commit("setVariableMappings", resp.VariableMappings);
  },
};
